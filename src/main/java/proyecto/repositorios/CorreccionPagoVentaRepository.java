package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.entidades.CorreccionPagoVenta;
import java.util.List;

public interface CorreccionPagoVentaRepository extends JpaRepository<CorreccionPagoVenta, Long> {
    List<CorreccionPagoVenta> findByVentaIdOrderByIdDesc(Long ventaId);
}
