package dev.pulsewatch.api;
import dev.pulsewatch.domain.HealthCheck;
import java.time.Instant;
import java.util.UUID;
public record HealthView(UUID id, String status, long responseTimeMs, Integer httpStatus, Instant checkedAt, int failureCount) {
    public static HealthView from(HealthCheck check) {
        return new HealthView(check.getId(), check.getStatus().name(), check.getResponseTimeMs(), check.getHttpStatus(), check.getCheckedAt(), check.getFailureCount());
    }
}
