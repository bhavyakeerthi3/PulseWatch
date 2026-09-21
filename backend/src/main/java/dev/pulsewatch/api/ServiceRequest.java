package dev.pulsewatch.api;
import jakarta.validation.constraints.*;
public record ServiceRequest(@NotBlank @Size(max=120) String name,@NotBlank @Size(max=255) String host,@Min(1) @Max(65535) int port,@NotBlank @Size(max=40) String environment,@NotBlank @Pattern(regexp="/.*") String healthEndpoint,boolean enabled) {}
