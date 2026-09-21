package dev.pulsewatch.repo;
import dev.pulsewatch.domain.MonitoredService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface ServiceRepository extends JpaRepository<MonitoredService,UUID> { boolean existsByName(String name); boolean existsByNameAndIdNot(String name, UUID id);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select s from MonitoredService s where s.id = :id")
 java.util.Optional<MonitoredService> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id); List<MonitoredService> findByEnabledTrue(); }
