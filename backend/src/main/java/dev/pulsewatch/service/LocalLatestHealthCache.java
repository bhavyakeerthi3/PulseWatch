package dev.pulsewatch.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("local")
public class LocalLatestHealthCache implements LatestHealthCache {
    private final ConcurrentHashMap<UUID, String> latest = new ConcurrentHashMap<>();

    @Override public void put(UUID serviceId, String value) { latest.put(serviceId, value); }
}
