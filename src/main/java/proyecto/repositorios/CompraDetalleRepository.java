package proyecto.repositorios;
import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.entidades.CompraDetalle;
import java.util.List;
public interface CompraDetalleRepository extends JpaRepository<CompraDetalle,Long> { List<CompraDetalle> findByCompraId(Long compraId); }
