package dev.pulsewatch.service;
import org.springframework.messaging.simp.SimpMessagingTemplate; import org.springframework.stereotype.Component;
@Component public class EventPublisher {
 private final SimpMessagingTemplate messaging; public EventPublisher(SimpMessagingTemplate messaging){this.messaging=messaging;}
 public void publish(String type,Object data){messaging.convertAndSend("/topic/events",new Event(type,data));}
 public record Event(String type,Object data) {}
}
