package dev.pulsewatch.api;

import dev.pulsewatch.domain.*; import dev.pulsewatch.repo.*; import dev.pulsewatch.service.MonitoringService;
import jakarta.validation.Valid; import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*; import java.util.*;

@RestController @RequestMapping("/api")
public class MonitoringController {
 private final MonitoringService monitoring; private final ServiceRepository services; private final HealthCheckRepository checks; private final AlertRepository alerts;
 public MonitoringController(MonitoringService monitoring,ServiceRepository services,HealthCheckRepository checks,AlertRepository alerts){this.monitoring=monitoring;this.services=services;this.checks=checks;this.alerts=alerts;}
 @PostMapping("/services") @ResponseStatus(HttpStatus.CREATED) public MonitoredService create(@Valid @RequestBody ServiceRequest r){return monitoring.create(r);}
 @GetMapping("/services") public List<ServiceView> services(){return monitoring.all().stream().map(s->{var history=monitoring.history(s.getId());var latest=history.isEmpty()?null:history.get(0);double uptime=history.isEmpty()?0:history.stream().filter(c->c.getStatus()!=HealthCheck.State.DOWN).count()*100.0/history.size();return ServiceView.from(s,latest==null?"PENDING":latest.getStatus().name(),latest==null?null:latest.getResponseTimeMs(),latest==null?null:latest.getCheckedAt(),uptime);}).toList();}
 @GetMapping("/services/{id}") public MonitoredService service(@PathVariable UUID id){return monitoring.get(id);}
 @PutMapping("/services/{id}") public MonitoredService update(@PathVariable UUID id,@Valid @RequestBody ServiceRequest r){return monitoring.update(id,r);}
 @DeleteMapping("/services/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id){monitoring.delete(id);}
 @PostMapping("/services/{id}/check") public HealthView check(@PathVariable UUID id){return HealthView.from(monitoring.checkNow(id));}
 @GetMapping("/services/{id}/health") public List<HealthView> health(@PathVariable UUID id){return monitoring.history(id).stream().map(HealthView::from).toList();}
 @GetMapping("/services/{id}/metrics") public Map<String,Object> metric(@PathVariable UUID id){var rows=monitoring.metricHistory(id);var checks=monitoring.history(id);var latencies=checks.stream().map(HealthCheck::getResponseTimeMs).sorted().toList();return Map.of("history",rows,"p95LatencyMs",percentile(latencies,.95),"p99LatencyMs",percentile(latencies,.99),"uptimePercent",checks.isEmpty()?0:checks.stream().filter(c->c.getStatus()!=HealthCheck.State.DOWN).count()*100.0/checks.size());}
 private long percentile(List<Long> values,double p){return values.isEmpty()?0:values.get((int)Math.min(values.size()-1,Math.ceil(p*values.size())-1));}
 @GetMapping("/alerts") public List<Alert> alerts(){return monitoring.allAlerts();}
 @PutMapping("/alerts/{id}/acknowledge") public Alert acknowledge(@PathVariable UUID id){return monitoring.transition(id,Alert.Lifecycle.ACKNOWLEDGED);}
 @PutMapping("/alerts/{id}/resolve") public Alert resolve(@PathVariable UUID id){return monitoring.transition(id,Alert.Lifecycle.RESOLVED);}
 @GetMapping("/incidents") public List<Incident> incidents(){return monitoring.allIncidents();}
 @GetMapping("/incidents/{id}") public Incident incident(@PathVariable UUID id){return monitoring.incident(id);}
 @GetMapping("/dashboard/summary") public Map<String,Object> summary(){return monitoring.summary();}
}
