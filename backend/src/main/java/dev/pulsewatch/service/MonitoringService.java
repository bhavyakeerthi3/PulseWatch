package dev.pulsewatch.service;

import dev.pulsewatch.api.ServiceRequest; import dev.pulsewatch.domain.*; import dev.pulsewatch.repo.*;
import org.springframework.beans.factory.annotation.Value; import org.springframework.data.domain.PageRequest; import org.springframework.http.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.client.RestClient;
import java.time.*; import java.util.*; import java.util.concurrent.*; import java.util.stream.*;

@Service
public class MonitoringService {
 private final ServiceRepository services; private final HealthCheckRepository checks; private final MetricRepository metrics; private final AlertRepository alerts; private final IncidentRepository incidents; private final LatestHealthCache latestHealthCache; private final EventPublisher events; private final RestClient http;
 private final org.springframework.transaction.support.TransactionTemplate transactions;
 private final int timeoutMs,retries,retryDelayMs; private final long latencyThreshold; private final int incidentMinutes;
 public MonitoringService(ServiceRepository services,HealthCheckRepository checks,MetricRepository metrics,AlertRepository alerts,IncidentRepository incidents,LatestHealthCache latestHealthCache,EventPublisher events, org.springframework.transaction.PlatformTransactionManager transactionManager,
   @Value("${pulsewatch.collector.timeout-ms}") int timeoutMs,@Value("${pulsewatch.collector.retries}") int retries,@Value("${pulsewatch.collector.retry-delay-ms}") int retryDelayMs,@Value("${pulsewatch.alert.latency-threshold-ms}") long latencyThreshold,@Value("${pulsewatch.alert.down-incident-minutes}") int incidentMinutes){
  this.transactions=new org.springframework.transaction.support.TransactionTemplate(transactionManager);this.services=services;this.checks=checks;this.metrics=metrics;this.alerts=alerts;this.incidents=incidents;this.latestHealthCache=latestHealthCache;this.events=events;this.timeoutMs=timeoutMs;this.retries=retries;this.retryDelayMs=retryDelayMs;this.latencyThreshold=latencyThreshold;this.incidentMinutes=incidentMinutes;
  this.http=RestClient.builder().requestFactory(factory()).build();
 }
 private org.springframework.http.client.ClientHttpRequestFactory factory(){var f=new org.springframework.http.client.SimpleClientHttpRequestFactory();f.setConnectTimeout(timeoutMs);f.setReadTimeout(timeoutMs);return f;}
 @Transactional public MonitoredService create(ServiceRequest r){if(services.existsByName(r.name()))throw new IllegalArgumentException("A service with this name already exists");return services.save(new MonitoredService(r.name(),r.host(),r.port(),r.environment(),r.healthEndpoint(),r.enabled()));}
 @Transactional public MonitoredService update(UUID id,ServiceRequest r){var s=locked(id);if(services.existsByNameAndIdNot(r.name(),id))throw new IllegalArgumentException("A service with this name already exists");s.update(r.name(),r.host(),r.port(),r.environment(),r.healthEndpoint(),r.enabled());return services.save(s);}
 public MonitoredService get(UUID id){return services.findById(id).orElseThrow(()->new NoSuchElementException("Service not found: "+id));}
 public List<MonitoredService> all(){return services.findAll();}
 private MonitoredService locked(UUID id){return services.findLockedById(id).orElseThrow(()->new NoSuchElementException("Service not found: "+id));}
 @Transactional public void delete(UUID id){var service=locked(id);incidents.deleteByServiceId(id);incidents.flush();alerts.deleteByServiceId(id);alerts.flush();metrics.deleteByServiceId(id);checks.deleteByServiceId(id);services.delete(service);events.publish("SERVICE_REMOVED",Map.of("name",service.getName()));}
 public List<HealthCheck> history(UUID id){get(id);return checks.findByServiceIdOrderByCheckedAtDesc(id,PageRequest.of(0,100));}
 public List<Metric> metricHistory(UUID id){get(id);return metrics.findByServiceIdOrderByRecordedAtDesc(id,PageRequest.of(0,100));}
 public void collectEnabled(){
  for(var service:services.findByEnabledTrue())try{
   transactions.executeWithoutResult(tx->{var current=locked(service.getId());if(current.isEnabled())check(current);});
  }catch(Exception failure){org.slf4j.LoggerFactory.getLogger(MonitoringService.class).warn("Collection failed for {}",service.getId(),failure);}
 }
 @Transactional public HealthCheck checkNow(UUID id){var service=locked(id);if(!service.isEnabled())throw new IllegalArgumentException("Enable monitoring before checking this service");return check(service);}
 private HealthCheck check(MonitoredService service){var old=checks.findFirstByServiceIdOrderByCheckedAtDesc(service.getId()).orElse(null);long start=System.nanoTime();Integer code=null;boolean ok=false;
  for(int attempt=0;attempt<=retries&&!ok;attempt++){try{var result=http.get().uri("http://"+service.getHost()+":"+service.getPort()+service.getHealthEndpoint()).retrieve().toBodilessEntity();code=result.getStatusCode().value();ok=result.getStatusCode().is2xxSuccessful();}
   catch(Exception e){if(e instanceof org.springframework.web.client.HttpStatusCodeException h)code=h.getStatusCode().value();if(attempt<retries)try{Thread.sleep(retryDelayMs);}catch(InterruptedException x){Thread.currentThread().interrupt();break;}}}
  long latency=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start);var state=!ok?HealthCheck.State.DOWN:latency>latencyThreshold?HealthCheck.State.DEGRADED:HealthCheck.State.UP;int failures=state==HealthCheck.State.DOWN?(old==null?1:old.getFailureCount()+1):0;
  var saved=checks.save(new HealthCheck(service,state,latency,code,failures));try{latestHealthCache.put(service.getId(),state+":"+latency+":"+saved.getCheckedAt());}catch(RuntimeException cacheFailure){org.slf4j.LoggerFactory.getLogger(MonitoringService.class).warn("Latest health cache unavailable for {}",service.getId());}
  if(old==null||old.getStatus()!=state)events.publish(old==null?"SERVICE_CHECKED":state==HealthCheck.State.UP?"SERVICE_RECOVERED":"SERVICE_STATE_CHANGED",Map.of("service",service,"status",state,"responseTimeMs",latency));
  recordMetrics(service,latency,ok);evaluate(service,saved);return saved;
 }
 private void recordMetrics(MonitoredService service,long latency,boolean ok){
  var previous=metrics.findFirstByServiceIdOrderByRecordedAtDesc(service.getId()).orElse(null);
  long count=previous==null?1:previous.getRequestCount()+1;
  long success=(previous==null?0:previous.getSuccessfulRequests())+(ok?1:0);
  double average=((previous==null?0:previous.getAverageLatencyMs()*(count-1))+latency)/count;
  metrics.save(new Metric(service,count,success,count-success,(count-success)*100.0/count,average));
 }
 private void resolve(Alert alert){alert.setStatus(Alert.Lifecycle.RESOLVED);incidents.findByAlertIdAndStatus(alert.getId(),"OPEN").forEach(incident->{incident.resolve();events.publish("INCIDENT_RESOLVED",incident);});events.publish("ALERT_RESOLVED",alert);}
 private void evaluate(MonitoredService s,HealthCheck h){String issue=h.getStatus()==HealthCheck.State.DOWN?"Service is DOWN":h.getStatus()==HealthCheck.State.DEGRADED?"Response time above "+latencyThreshold+" ms":null;if(issue==null){alerts.findByServiceIdAndStatusIn(s.getId(),List.of(Alert.Lifecycle.OPEN,Alert.Lifecycle.ACKNOWLEDGED)).forEach(a->{resolve(a);});return;}
  alerts.findByServiceIdAndStatusIn(s.getId(),List.of(Alert.Lifecycle.OPEN,Alert.Lifecycle.ACKNOWLEDGED)).stream().filter(a->!a.getMessage().equals(issue)).forEach(this::resolve);
  var ongoing=alerts.findFirstByServiceIdAndMessageAndStatusInOrderByCreatedAtDesc(s.getId(),issue,List.of(Alert.Lifecycle.OPEN,Alert.Lifecycle.ACKNOWLEDGED));var alert=ongoing.orElseGet(()->{var a=alerts.save(new Alert(s,issue,h.getStatus()==HealthCheck.State.DOWN?Alert.Severity.CRITICAL:Alert.Severity.WARNING));events.publish("ALERT_CREATED",a);return a;});
  if(h.getStatus()==HealthCheck.State.DOWN&&!incidents.existsByAlertId(alert.getId())){var history=checks.findByServiceIdOrderByCheckedAtDesc(s.getId());var firstFailure=history.stream().takeWhile(c->c.getStatus()==HealthCheck.State.DOWN).reduce((first,next)->next).orElse(h);if(Duration.between(firstFailure.getCheckedAt(),h.getCheckedAt()).toMinutes()>=incidentMinutes){var incident=incidents.save(new Incident(s,alert));events.publish("INCIDENT_CREATED",incident);}}
 }
 public List<Alert> allAlerts(){return alerts.findAllByOrderByCreatedAtDesc();}
 @Transactional public Alert transition(UUID id,Alert.Lifecycle status){var a=alerts.findById(id).orElseThrow(()->new NoSuchElementException("Alert not found: "+id));if(a.getStatus()==Alert.Lifecycle.RESOLVED&&status!=Alert.Lifecycle.RESOLVED)throw new IllegalArgumentException("A resolved alert cannot be acknowledged");if(status==Alert.Lifecycle.RESOLVED)resolve(a);else{a.setStatus(status);events.publish("ALERT_UPDATED",a);}return a;}
 public List<Incident> allIncidents(){return incidents.findAllByOrderByCreatedAtDesc();}
 public Incident incident(UUID id){return incidents.findById(id).orElseThrow(()->new NoSuchElementException("Incident not found: "+id));}
 public Map<String,Object> summary(){var ss=services.findAll();long up=0,down=0,degraded=0;List<Long> latency=new ArrayList<>();for(var s:ss){if(!s.isEnabled())continue;var c=checks.findFirstByServiceIdOrderByCheckedAtDesc(s.getId()).orElse(null);if(c!=null){switch(c.getStatus()){case UP->up++;case DOWN->down++;case DEGRADED->degraded++;}latency.add(c.getResponseTimeMs());}}
  double avg=latency.stream().mapToLong(Long::longValue).average().orElse(0);return Map.of("totalServices",ss.size(),"up",up,"down",down,"degraded",degraded,"activeAlerts",alerts.countByStatusIn(List.of(Alert.Lifecycle.OPEN,Alert.Lifecycle.ACKNOWLEDGED)),"criticalAlerts",alerts.countBySeverityAndStatusIn(Alert.Severity.CRITICAL,List.of(Alert.Lifecycle.OPEN,Alert.Lifecycle.ACKNOWLEDGED)),"openIncidents",incidents.countByStatus("OPEN"),"averageLatencyMs",avg);}
}
