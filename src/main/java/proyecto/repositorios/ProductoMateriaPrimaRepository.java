package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.entidades.MateriaPrima;
import proyecto.entidades.Producto;
import proyecto.entidades.ProductoMateriaPrima;

public interface ProductoMateriaPrimaRepository
        extends JpaRepository<ProductoMateriaPrima, Long> {

    boolean existsByProductoAndMateriaPrima(Producto producto, MateriaPrima materiaPrima);

}




