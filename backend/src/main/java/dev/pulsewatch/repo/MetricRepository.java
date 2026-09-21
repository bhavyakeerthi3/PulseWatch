package dev.pulsewatch.repo;
import dev.pulsewatch.domain.Metric; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface MetricRepository extends JpaRepository<Metric,UUID> { List<Metric> findByServiceIdOrderByRecordedAtDesc(UUID serviceId); }
