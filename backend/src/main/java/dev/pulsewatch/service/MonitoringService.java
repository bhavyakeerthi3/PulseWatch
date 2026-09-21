package dev.pulsewatch.service;

import dev.pulsewatch.api.ServiceRequest; import dev.pulsewatch.domain.*; import dev.pulsewatch.repo.*;
import org.springframework.beans.factory.annotation.Value; import org.springframework.data.domain.PageRequest; import org.springframework.http.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.client.RestClient;
import java.time.*; import java.util.*; import java.util.concurrent.*; import java.util.stream.*;

@Service
public class MonitoringService {
 private final ServiceRepository services; private final HealthCheckRepository checks; private final MetricRepository metrics; private final AlertRepository alerts; private final IncidentRepository incidents; private final LatestHealthCache latestHealthCache; private final EventPublisher events; private final RestClient http;
 private final int timeoutMs,retries,retryDelayMs; private final long latencyThreshold; private final int incidentMinutes;
 public MonitoringService(ServiceRepository services,HealthCheckRepository checks,MetricRepository metrics,AlertRepository alerts,IncidentRepository incidents,LatestHealthCache latestHealthCache,EventPublisher events,
   @Value("${pulsewatch.collector.timeout-ms}") int timeoutMs,@Value("${pulsewatch.collector.retries}") int retries,@Value("${pulsewatch.collector.retry-delay-ms}") int retryDelayMs,@Value("${pulsewatch.alert.latency-threshold-ms}") long latencyThreshold,@Value("${pulsewatch.alert.down-incident-minutes}") int incidentMinutes){
  this.services=services;this.checks=checks;this.metrics=metrics;this.alerts=alerts;this.incidents=incidents;this.latestHealthCache=latestHealthCache;this.events=events;this.timeoutMs=timeoutMs;this.retries=retries;this.retryDelayMs=retryDelayMs;this.latencyThreshold=latencyThreshold;this.incidentMinutes=incidentMinutes;
  this.http=RestClient.builder().requestFactory(factory()).build();
 }
 private org.springframework.http.client.ClientHttpRequestFactory factory(){var f=new org.springframework.http.client.SimpleClientHttpRequestFactory();f.setConnectTimeout(timeoutMs);f.setReadTimeout(timeoutMs);return f;}
 @Transactional public MonitoredService create(ServiceRequest r){if(services.existsByName(r.name()))throw new IllegalArgumentException("A service with this name already exists");return services.save(new MonitoredService(r.name(),r.host(),r.port(),r.environment(),r.healthEndpoint(),r.enabled()));}
 @Transactional public MonitoredService update(UUID id,ServiceRequest r){var s=get(id);s.update(r.name(),r.host(),r.port(),r.environment(),r.healthEndpoint(),r.enabled());return services.save(s);}
 public MonitoredService get(UUID id){return services.findById(id).orElseThrow(()->new NoSuchElementException("Service not found: "+id));}
 public List<MonitoredService> all(){return services.findAll();}
 @Transactional public void delete(UUID id){services.delete(get(id));}
 public List<HealthCheck> history(UUID id){get(id);return checks.findByServiceIdOrderByCheckedAtDesc(id,PageRequest.of(0,100));}
 public List<Metric> metricHistory(UUID id){get(id);return metrics.findByServiceIdOrderByRecordedAtDesc(id);}
 @Transactional public void collectEnabled(){for(var s:services.findByEnabledTrue())try{check(s);}catch(Exception e){System.err.println("Monitor cycle failed for service "+s.getId()+": "+e.getClass().getSimpleName());}}
 @Transactional public HealthCheck check(MonitoredService service){var old=checks.findFirstByServiceIdOrderByCheckedAtDesc(service.getId()).orElse(null);long start=System.nanoTime();Integer code=null;boolean ok=false;
  for(int attempt=0;attempt<=retries&&!ok;attempt++){try{var result=http.get().uri("http://"+service.getHost()+":"+service.getPort()+service.getHealthEndpoint()).retrieve().toBodilessEntity();code=result.getStatusCode().value();ok=result.getStatusCode().is2xxSuccessful();}
   catch(Exception e){if(e instanceof org.springframework.web.client.HttpStatusCodeException h)code=h.getStatusCode().value();if(attempt<retries)try{Thread.sleep(retryDelayMs);}catch(InterruptedException x){Thread.currentThread().interrupt();break;}}}
  long latency=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);var state=!ok?HealthCheck.State.DOWN:latency>latencyThreshold?HealthCheck.State.DEGRADED:HealthCheck.State.UP;int failures=state==HealthCheck.State.DOWN?(old==null?1:old.getFailureCount()+1):0;
  var saved=checks.save(new HealthCheck(service,state,latency,code,failures));latestHealthCache.put(service.getId(),state+":"+latency+":"+saved.getCheckedAt());
  if(old==null||old.getStatus()!=state)events.publish(state==HealthCheck.State.UP?"SERVICE_RECOVERED":"SERVICE_STATE_CHANGED",saved);
  recordMetrics(service,latency,ok);evaluate(service,saved);return saved;
 }
 private void recordMetrics(MonitoredService s,long latency,boolean ok){var rows=checks.findByServiceIdOrderByCheckedAtDesc(s.getId());long count=rows.size(),success=rows.stream().filter(x->x.getStatus()!=HealthCheck.State.DOWN).count();double error=(count-success)*100.0/count;double avg=rows.stream().mapToLong(HealthCheck::getResponseTimeMs).average().orElse(0);metrics.save(new Metric(s,count,success,count-success,error,avg));}
 private void evaluate(MonitoredService s,HealthCheck h){String issue=h.getStatus()==HealthCheck.State.DOWN?"Service is DOWN":h.getStatus()==HealthCheck.State.DEGRADED?"Response time above "+latencyThreshold+" ms":null;if(issue==null){alerts.findByServiceIdAndStatusIn(s.getId(),List.of(Alert.Lifecycle.OPEN,Alert.Lifecycle.ACKNOWLEDGED)).forEach(a->{a.setStatus(Alert.Lifecycle.RESOLVED);events.publish("ALERT_RESOLVED",a);});return;}
  var ongoing=alerts.findFirstByServiceIdAndMessageAndStatusInOrderByCreatedAtDesc(s.getId(),issue,List.of(Alert.Lifecycle.OPEN,Alert.Lifecycle.ACKNOWLEDGED));var alert=ongoing.orElseGet(()->{var a=alerts.save(new Alert(s,issue,h.getStatus()==HealthCheck.State.DOWN?Alert.Severity.CRITICAL:Alert.Severity.WARNING));events.publish("ALERT_CREATED",a);return a;});
  if(h.getStatus()==HealthCheck.State.DOWN&&!incidents.existsByAlertId(alert.getId())){var history=checks.findByServiceIdOrderByCheckedAtDesc(s.getId());var firstFailure=history.stream().takeWhile(c->c.getStatus()==HealthCheck.State.DOWN).reduce((first,next)->next).orElse(h);if(Duration.between(firstFailure.getCheckedAt(),h.getCheckedAt()).toMinutes()>=incidentMinutes){var incident=incidents.save(new Incident(s,alert));events.publish("INCIDENT_CREATED",incident);}}
 }
 public List<Alert> allAlerts(){return alerts.findAllByOrderByCreatedAtDesc();}
 @Transactional public Alert transition(UUID id,Alert.Lifecycle status){var a=alerts.findById(id).orElseThrow(()->new NoSuchElementException("Alert not found: "+id));a.setStatus(status);events.publish("ALERT_UPDATED",a);return a;}
 public List<Incident> allIncidents(){return incidents.findAllByOrderByCreatedAtDesc();}
 public Incident incident(UUID id){return incidents.findById(id).orElseThrow(()->new NoSuchElementException("Incident not found: "+id));}
 public Map<String,Object> summary(){var ss=services.findAll();long up=0,down=0,degraded=0;List<Long> latency=new ArrayList<>();for(var s:ss){var c=checks.findFirstByServiceIdOrderByCheckedAtDesc(s.getId()).orElse(null);if(c!=null){switch(c.getStatus()){case UP->up++;case DOWN->down++;case DEGRADED->degraded++;}latency.add(c.getResponseTimeMs());}}
  double avg=latency.stream().mapToLong(Long::longValue).average().orElse(0);return Map.of("totalServices",ss.size(),"up",up,"down",down,"degraded",degraded,"activeAlerts",alerts.countByStatusIn(List.of(Alert.Lifecycle.OPEN,Alert.Lifecycle.ACKNOWLEDGED)),"criticalAlerts",alerts.countBySeverityAndStatusIn(Alert.Severity.CRITICAL,List.of(Alert.Lifecycle.OPEN,Alert.Lifecycle.ACKNOWLEDGED)),"openIncidents",incidents.countByStatus("OPEN"),"averageLatencyMs",avg);}
}
