package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.dto.InventarioDTO;
import proyecto.dto.PerdidasDetalleDTO;
import proyecto.entidades.Inventario;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {
    // Listar todo el inventario de una sede
    @Query("""
        SELECT i
        FROM Inventario i
        JOIN i.producto p
        JOIN i.sede s
        WHERE s.id = :sedeId
          AND p.activo = true
          AND p.empresa.nit = s.empresa.nit
        ORDER BY p.codigo ASC
    """)
    List<Inventario> findVisiblesBySedeIdOrderByProductoCodigoAsc(@Param("sedeId") Long sedeId);

    // Obtener inventario de un producto especÃ­fico en una sede
    @Query("""
        SELECT i
        FROM Inventario i
        JOIN i.producto p
        JOIN i.sede s
        WHERE s.id = :sedeId
          AND p.codigo = :productoId
          AND p.empresa.nit = s.empresa.nit
    """)
    Optional<Inventario> findVisibleByProductoCodigoAndSedeId(
            @Param("productoId") Long productoId,
            @Param("sedeId") Long sedeId
    );

    // Verificar si existe inventario para producto + sede
    boolean existsByProductoCodigoAndSedeId(Long productoId, Long sedeId);

    @Query("""
SELECT new proyecto.dto.InventarioDTO(
    i.id,
    p.codigo,
    p.nombre,
    COALESCE(i.stockActual, 0),
    COALESCE(i.entradas, 0),
    COALESCE(i.salidas, 0),
    COALESCE(i.perdidas, 0),
    COALESCE(i.stockMinimo, 0),
    p.precioVenta
)
FROM Producto p
JOIN p.empresa e
JOIN Sede s
    ON s.id = :sedeId
LEFT JOIN Inventario i
    ON i.producto = p
    AND i.sede.id = :sedeId
WHERE p.activo = true
  AND e.nit = s.empresa.nit
""")
    List<InventarioDTO> listarInventarioCompletoPorSede(@Param("sedeId") Long sedeId);

    @Query("""
        SELECT COALESCE(SUM(i.stockActual), 0)
        FROM Inventario i
    """)
    Integer stockTotal();

    @Query("""
        SELECT COALESCE(SUM(i.stockActual * p.precioProduccion), 0)
        FROM Inventario i
        JOIN i.producto p
    """)
    Double valorInventario();

    @Query("""
        SELECT COALESCE(SUM(i.stockActual), 0)
        FROM Inventario i
        WHERE i.sede.id = :sedeId
    """)
    Integer stockPorSede(@Param("sedeId") Long sedeId);

    @Query("""
        SELECT COALESCE(SUM(i.stockActual * p.precioProduccion), 0)
        FROM Inventario i
        JOIN i.producto p
        WHERE i.sede.id = :sedeId
    """)
    Double valorInventarioPorSede(@Param("sedeId") Long sedeId);

    @Query("""
        SELECT COALESCE(SUM(i.stockActual), 0)
        FROM Inventario i
        WHERE i.sede.empresa.nit = :empresaNit
    """)
    Integer stockTotalPorEmpresa(@Param("empresaNit") Long empresaNit);

    @Query("""
        SELECT COALESCE(SUM(i.stockActual * p.precioProduccion), 0)
        FROM Inventario i
        JOIN i.producto p
        WHERE i.sede.empresa.nit = :empresaNit
    """)
    Double valorInventarioPorEmpresa(@Param("empresaNit") Long empresaNit);

    @Query("""
    SELECT new proyecto.dto.PerdidasDetalleDTO(
        m.fecha,
        m.producto.nombre,
        m.cantidad,
        m.observacion
    )
    FROM MovimientoInventario m
    WHERE m.tipo = 'PERDIDA'
      AND m.sede.id = :sedeId
      AND m.fecha BETWEEN :fechaInicio AND :fechaFin
    ORDER BY m.fecha
""")
    List<PerdidasDetalleDTO> obtenerPerdidasDetalladasPorRango(
            @Param("sedeId") Long sedeId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin
    );


}



