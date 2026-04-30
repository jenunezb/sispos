package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.MovimientoProduccion;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoProduccionRepository extends JpaRepository<MovimientoProduccion, Long> {

    boolean existsByVendedorCodigo(Long vendedorId);

    List<MovimientoProduccion> findBySedeIdAndFechaBetweenOrderByFechaAsc(Long sedeId, LocalDateTime inicio, LocalDateTime fin);
}
