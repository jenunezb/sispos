package proyecto.dian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.dian.model.DianTransmissionAttempt;

public interface DianTransmissionAttemptRepository extends JpaRepository<DianTransmissionAttempt, Long> {
}
