package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.entidades.MateriaPrima;
import proyecto.entidades.MateriaPrimaSede;
import proyecto.entidades.Sede;

import java.util.Optional;

public interface MateriaPrimaSedeRepository extends JpaRepository<MateriaPrimaSede, Long> {

    Optional<MateriaPrimaSede> findByMateriaPrimaAndSede(MateriaPrima materiaPrima, Sede sede);

    // También útil si quieres buscar por IDs directamente
    Optional<MateriaPrimaSede> findByMateriaPrima_CodigoAndSede_Id(Long materiaPrimaId, Long sedeId);
}



