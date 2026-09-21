package dev.pulsewatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.Instant;

@Component
public class EventPublisher {
    private final SimpMessagingTemplate messaging;
    private final ObjectMapper mapper;
    public EventPublisher(SimpMessagingTemplate messaging, ObjectMapper mapper) {
        this.messaging = messaging; this.mapper = mapper;
    }
    public void publish(String type, Object data) {
        // Materialize entity data while the persistence context is open.
        var event = new Event(type, mapper.valueToTree(data), Instant.now());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { send(event); }
            });
        } else send(event);
    }
    private void send(Event event) {
        try { messaging.convertAndSend("/topic/events", event); }
        catch (RuntimeException failure) {
            org.slf4j.LoggerFactory.getLogger(EventPublisher.class).warn("Event delivery failed; REST polling remains available", failure);
        }
    }
    public record Event(String type, Object data, Instant occurredAt) {}
}
