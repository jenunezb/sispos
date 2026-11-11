package proyecto.repositorios;
import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.entidades.ProductoComplemento;
import java.util.List;
public interface ProductoComplementoRepository extends JpaRepository<ProductoComplemento,Long> {
 List<ProductoComplemento> findByProductoCodigoAndActivoTrueOrderByNombreAsc(Long productoId);
 void deleteByProductoCodigo(Long productoId);
}
