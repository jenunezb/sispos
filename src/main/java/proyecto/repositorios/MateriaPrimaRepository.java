package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.entidades.MateriaPrima;

import java.util.Optional;

public interface MateriaPrimaRepository extends JpaRepository<MateriaPrima, Long> {

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<MateriaPrima> findByNombreIgnoreCase(String nombre);

}