package dev.pulsewatch.api;

import dev.pulsewatch.domain.MonitoredService;
import java.time.Instant;
import java.util.UUID;

public record ServiceView(UUID id, String name, String host, int port, String environment,
                          String healthEndpoint, boolean enabled, Instant createdAt,
                          String status, Long responseTimeMs, Instant lastCheckedAt, double uptimePercent) {
    public static ServiceView from(MonitoredService service, String status, Long responseTimeMs,
                                   Instant lastCheckedAt, double uptimePercent) {
        return new ServiceView(service.getId(), service.getName(), service.getHost(), service.getPort(),
                service.getEnvironment(), service.getHealthEndpoint(), service.isEnabled(), service.getCreatedAt(),
                status, responseTimeMs, lastCheckedAt, uptimePercent);
    }
}
