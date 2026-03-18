package proyecto.repositorios;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.SuscripcionSede;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuscripcionSedeRepository extends JpaRepository<SuscripcionSede, Long> {

    @EntityGraph(attributePaths = {"sede", "sede.empresa"})
    List<SuscripcionSede> findAll();

    @EntityGraph(attributePaths = {"sede", "sede.empresa", "pagos"})
    Optional<SuscripcionSede> findBySedeId(Long sedeId);
}
