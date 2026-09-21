package dev.pulsewatch.repo;
import dev.pulsewatch.domain.MonitoredService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface ServiceRepository extends JpaRepository<MonitoredService,UUID> { boolean existsByName(String name); List<MonitoredService> findByEnabledTrue(); }
