package dev.pulsewatch.service;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.UUID;

@Component
@Profile("!local")
public class RedisLatestHealthCache implements LatestHealthCache {
    private final StringRedisTemplate redis;

    public RedisLatestHealthCache(StringRedisTemplate redis) { this.redis = redis; }

    @Override public void put(UUID serviceId, String value) {
        redis.opsForValue().set("pulsewatch:service:" + serviceId + ":latest", value, Duration.ofMinutes(5));
    }
}
