package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.InventarioProduccion;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioProduccionRepository extends JpaRepository<InventarioProduccion, Long> {

    Optional<InventarioProduccion> findByProductoCodigoAndSedeId(Long productoId, Long sedeId);

    List<InventarioProduccion> findBySedeIdAndProductoActivoTrueOrderByProductoCodigoAsc(Long sedeId);
}
