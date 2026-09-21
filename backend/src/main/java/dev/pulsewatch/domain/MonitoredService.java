package dev.pulsewatch.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="monitored_services", indexes=@Index(name="idx_service_enabled", columnList="enabled"))
public class MonitoredService {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false, unique=true, length=120) private String name;
    @Column(nullable=false, length=255) private String host;
    @Column(nullable=false) private int port;
    @Column(nullable=false, length=40) private String environment;
    @Column(name="health_endpoint", nullable=false, length=255) private String healthEndpoint;
    @Column(nullable=false) private boolean enabled=true;
    @Column(nullable=false, updatable=false) private Instant createdAt=Instant.now();
    protected MonitoredService() {}
    public MonitoredService(String name,String host,int port,String environment,String healthEndpoint,boolean enabled){this.name=name;this.host=host;this.port=port;this.environment=environment;this.healthEndpoint=healthEndpoint;this.enabled=enabled;}
    public UUID getId(){return id;} public String getName(){return name;} public String getHost(){return host;} public int getPort(){return port;} public String getEnvironment(){return environment;} public String getHealthEndpoint(){return healthEndpoint;} public boolean isEnabled(){return enabled;} public Instant getCreatedAt(){return createdAt;}
    public void update(String name,String host,int port,String environment,String endpoint,boolean enabled){this.name=name;this.host=host;this.port=port;this.environment=environment;this.healthEndpoint=endpoint;this.enabled=enabled;}
}
