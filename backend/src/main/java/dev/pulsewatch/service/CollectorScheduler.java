package dev.pulsewatch.service;
import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component;
@Component public class CollectorScheduler {
 private final MonitoringService monitoring; public CollectorScheduler(MonitoringService monitoring){this.monitoring=monitoring;}
 @Scheduled(fixedDelayString="${pulsewatch.collector.interval-ms}") public void collect(){monitoring.collectEnabled();}
}
