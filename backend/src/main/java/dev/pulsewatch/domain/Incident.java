package dev.pulsewatch.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="incidents",indexes=@Index(name="idx_incident_status",columnList="status"))
public class Incident {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="service_id",nullable=false) private MonitoredService service;
    @ManyToOne(optional=false) @JoinColumn(name="alert_id",nullable=false,unique=true) private Alert alert;
    @Column(nullable=false,length=24) private String status="OPEN";
    @Column(nullable=false,updatable=false) private Instant createdAt=Instant.now();
    private Instant resolvedAt;
    public Instant getResolvedAt(){return resolvedAt;}
    public void resolve(){this.status="RESOLVED";this.resolvedAt=Instant.now();}
    protected Incident() {}
    public Incident(MonitoredService service,Alert alert){this.service=service;this.alert=alert;}
    public UUID getId(){return id;} public MonitoredService getService(){return service;} public Alert getAlert(){return alert;} public String getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
}
