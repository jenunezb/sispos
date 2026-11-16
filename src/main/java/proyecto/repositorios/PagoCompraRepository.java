package proyecto.repositorios;
import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.entidades.PagoCompra;
import java.util.List;
public interface PagoCompraRepository extends JpaRepository<PagoCompra,Long> { List<PagoCompra> findByCompraIdOrderByFechaAscIdAsc(Long compraId); }
