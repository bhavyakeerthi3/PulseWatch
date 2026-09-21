package dev.pulsewatch.repo;
import dev.pulsewatch.domain.Metric; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface MetricRepository extends JpaRepository<Metric,UUID> { void deleteByServiceId(UUID serviceId);  List<Metric> findByServiceIdOrderByRecordedAtDesc(UUID serviceId, org.springframework.data.domain.Pageable page); Optional<Metric> findFirstByServiceIdOrderByRecordedAtDesc(UUID serviceId); }
