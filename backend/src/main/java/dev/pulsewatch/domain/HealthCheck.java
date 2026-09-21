package dev.pulsewatch.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="health_checks", indexes={@Index(name="idx_check_service_time",columnList="service_id,checked_at"),@Index(name="idx_check_time",columnList="checked_at")})
public class HealthCheck {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="service_id",nullable=false) private MonitoredService service;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private State status;
    @Column(nullable=false) private long responseTimeMs;
    private Integer httpStatus;
    @Column(nullable=false) private Instant checkedAt=Instant.now();
    @Column(nullable=false) private int failureCount;
    protected HealthCheck() {}
    public HealthCheck(MonitoredService service,State status,long responseTimeMs,Integer httpStatus,int failureCount){this.service=service;this.status=status;this.responseTimeMs=responseTimeMs;this.httpStatus=httpStatus;this.failureCount=failureCount;}
    public UUID getId(){return id;} public MonitoredService getService(){return service;} public State getStatus(){return status;} public long getResponseTimeMs(){return responseTimeMs;} public Integer getHttpStatus(){return httpStatus;} public Instant getCheckedAt(){return checkedAt;} public int getFailureCount(){return failureCount;}
    public enum State { UP, DOWN, DEGRADED }
}
