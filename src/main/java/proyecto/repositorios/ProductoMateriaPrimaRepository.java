package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import proyecto.entidades.MateriaPrima;
import proyecto.entidades.Producto;
import proyecto.entidades.ProductoMateriaPrima;

import java.util.List;

public interface ProductoMateriaPrimaRepository
        extends JpaRepository<ProductoMateriaPrima, Long> {

    boolean existsByProductoAndMateriaPrima(Producto producto, MateriaPrima materiaPrima);

    @Query("SELECT CASE WHEN COUNT(pmp) > 0 THEN true ELSE false END " +
            "FROM ProductoMateriaPrima pmp " +
            "WHERE pmp.materiaPrima.id = :materiaPrimaId " +
            "AND pmp.producto.id = :productoId")
    boolean existsByMateriaPrimaIdAndProductoId(
            @Param("materiaPrimaId") Long materiaPrimaId,
            @Param("productoId") Long productoId
    );

    List<ProductoMateriaPrima> findByProductoCodigo(Long productoCodigo);
}




