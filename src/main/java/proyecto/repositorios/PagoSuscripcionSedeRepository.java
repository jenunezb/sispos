package proyecto.repositorios;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.PagoSuscripcionSede;

import java.util.List;

@Repository
public interface PagoSuscripcionSedeRepository extends JpaRepository<PagoSuscripcionSede, Long> {

    @EntityGraph(attributePaths = {"sede", "sede.empresa", "suscripcion"})
    List<PagoSuscripcionSede> findAllByOrderByFechaPagoDescIdDesc();

    @EntityGraph(attributePaths = {"sede", "sede.empresa", "suscripcion"})
    List<PagoSuscripcionSede> findBySedeIdOrderByFechaPagoDescIdDesc(Long sedeId);
}
