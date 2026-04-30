package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Producto;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByActivoTrueAndEmpresaNitOrderByCodigoAsc(Long empresaNit);

    @Query("""
        SELECT DISTINCT p
        FROM Producto p
        JOIN Inventario i ON i.producto = p
        JOIN i.sede s
        WHERE p.activo = true
          AND p.empresa.nit = :empresaNit
          AND s.id = :sedeId
          AND s.empresa.nit = :empresaNit
        ORDER BY p.codigo ASC
    """)
    List<Producto> findActivosByEmpresaNitAndSedeIdOrderByCodigoAsc(
            @Param("empresaNit") Long empresaNit,
            @Param("sedeId") Long sedeId
    );

}
