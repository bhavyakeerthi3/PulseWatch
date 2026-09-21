package dev.pulsewatch.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="alerts",indexes={@Index(name="idx_alert_status",columnList="status"),@Index(name="idx_alert_service_status",columnList="service_id,status")})
public class Alert {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="service_id",nullable=false) private MonitoredService service;
    @Column(nullable=false,length=500) private String message;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private Severity severity;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private Lifecycle status=Lifecycle.OPEN;
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    private Instant updatedAt=Instant.now();
    protected Alert() {}
    public Alert(MonitoredService service,String message,Severity severity){this.service=service;this.message=message;this.severity=severity;}
    public UUID getId(){return id;} public MonitoredService getService(){return service;} public String getMessage(){return message;} public Severity getSeverity(){return severity;} public Lifecycle getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
    public void setStatus(Lifecycle status){this.status=status;this.updatedAt=Instant.now();}
    public enum Severity { INFO, WARNING, CRITICAL } public enum Lifecycle { OPEN, ACKNOWLEDGED, RESOLVED }
}
