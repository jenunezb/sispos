package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.entidades.Producto;
import proyecto.entidades.ProductoMateriaPrima;

import java.util.List;

public interface ProductoMateriaPrimaRepository
        extends JpaRepository<ProductoMateriaPrima, Long> {

    boolean existsByProducto_Codigo(Long codigo);
}




