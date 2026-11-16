package proyecto.dian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.dian.model.DianConfiguration;
import proyecto.dian.model.DianEnvironment;

import java.util.Optional;

public interface DianConfigurationRepository extends JpaRepository<DianConfiguration, Long> {
    Optional<DianConfiguration> findByEmpresaNitAndEnvironment(Long empresaNit, DianEnvironment environment);
}
