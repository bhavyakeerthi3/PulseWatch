package dev.pulsewatch.repo;
import dev.pulsewatch.domain.Incident; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface IncidentRepository extends JpaRepository<Incident,UUID> { List<Incident> findAllByOrderByCreatedAtDesc(); long countByStatus(String status); }
