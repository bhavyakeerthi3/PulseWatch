package dev.pulsewatch.api;
import jakarta.validation.constraints.*;
public record ServiceRequest(@NotBlank @Size(max=120) String name,@NotBlank @Size(max=255) @Pattern(regexp="[a-zA-Z0-9.-]+", message="must be a hostname or IPv4 address without a scheme or path") String host,@Min(1) @Max(65535) int port,@NotBlank @Size(max=40) String environment,@NotBlank @Size(max=255) @Pattern(regexp="/[^\\s#]*", message="must start with / and contain no spaces or fragment") String healthEndpoint,boolean enabled) {}
