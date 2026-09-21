package dev.pulsewatch.repo;
import dev.pulsewatch.domain.HealthCheck; import org.springframework.data.jpa.repository.JpaRepository; import org.springframework.data.domain.Pageable; import java.util.*;
public interface HealthCheckRepository extends JpaRepository<HealthCheck,UUID> { void deleteByServiceId(UUID serviceId);  List<HealthCheck> findByServiceIdOrderByCheckedAtDesc(UUID serviceId,Pageable page); Optional<HealthCheck> findFirstByServiceIdOrderByCheckedAtDesc(UUID serviceId); List<HealthCheck> findByServiceIdOrderByCheckedAtDesc(UUID serviceId); }
