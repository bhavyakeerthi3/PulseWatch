package dev.pulsewatch.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AlertTest {
    @Test void newAlertStartsOpenAndCanBeAcknowledgedThenResolved() {
        var service = new MonitoredService("payments", "localhost", 8081, "dev", "/health", true);
        var alert = new Alert(service, "Service is DOWN", Alert.Severity.CRITICAL);

        assertEquals(Alert.Lifecycle.OPEN, alert.getStatus());
        alert.setStatus(Alert.Lifecycle.ACKNOWLEDGED);
        assertEquals(Alert.Lifecycle.ACKNOWLEDGED, alert.getStatus());
        alert.setStatus(Alert.Lifecycle.RESOLVED);
        assertEquals(Alert.Lifecycle.RESOLVED, alert.getStatus());
    }
}
