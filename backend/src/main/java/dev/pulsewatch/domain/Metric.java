package dev.pulsewatch.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="metrics",indexes=@Index(name="idx_metric_service_time",columnList="service_id,recorded_at"))
public class Metric {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="service_id",nullable=false) private MonitoredService service;
    @Column(nullable=false) private long requestCount;
    @Column(nullable=false) private long successfulRequests;
    @Column(nullable=false) private long failedRequests;
    @Column(nullable=false) private double errorRate;
    @Column(nullable=false) private double averageLatencyMs;
    @Column(nullable=false) private Instant recordedAt=Instant.now();
    protected Metric() {}
    public Metric(MonitoredService service,long count,long success,long failed,double errorRate,double latency){this.service=service;this.requestCount=count;this.successfulRequests=success;this.failedRequests=failed;this.errorRate=errorRate;this.averageLatencyMs=latency;}
    public MonitoredService getService(){return service;} public Instant getRecordedAt(){return recordedAt;} public long getRequestCount(){return requestCount;} public long getSuccessfulRequests(){return successfulRequests;} public long getFailedRequests(){return failedRequests;} public double getErrorRate(){return errorRate;} public double getAverageLatencyMs(){return averageLatencyMs;}
}
