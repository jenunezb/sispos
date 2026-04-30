package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import proyecto.entidades.MateriaPrima;
import proyecto.entidades.Producto;
import proyecto.entidades.ProductoMateriaPrima;

import java.util.List;
import java.util.Optional;

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

    boolean existsByMateriaPrimaSedeIdAndProductoCodigo(Long materiaPrimaSedeId, Long productoCodigo);

    List<ProductoMateriaPrima> findByProductoCodigoAndMateriaPrimaSedeSedeId(Long productoCodigo, Long sedeId);

    List<ProductoMateriaPrima> findByMateriaPrimaCodigoOrderByProductoNombreAsc(Long materiaPrimaId);

    List<ProductoMateriaPrima> findByMateriaPrimaSedeIdOrderByProductoNombreAsc(Long materiaPrimaSedeId);

    Optional<ProductoMateriaPrima> findByMateriaPrimaSedeIdAndProductoCodigo(Long materiaPrimaSedeId, Long productoCodigo);
}




