package dev.pulsewatch.repo;
import dev.pulsewatch.domain.Incident; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface IncidentRepository extends JpaRepository<Incident,UUID> { void deleteByServiceId(UUID serviceId);  List<Incident> findAllByOrderByCreatedAtDesc(); long countByStatus(String status); List<Incident> findByAlertIdAndStatus(UUID alertId, String status); boolean existsByAlertId(UUID alertId); }
