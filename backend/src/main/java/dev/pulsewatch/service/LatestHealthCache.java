package dev.pulsewatch.service;

import java.util.UUID;

public interface LatestHealthCache {
    void put(UUID serviceId, String value);
}
