package dev.pulsewatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PulseWatchApplication {
    public static void main(String[] args) { SpringApplication.run(PulseWatchApplication.class, args); }
}
