package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import proyecto.entidades.MovimientoInventario;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioRepository
        extends JpaRepository<MovimientoInventario, Long> {
    @Query("""
SELECT 
    m.producto.id,
    SUM(CASE WHEN m.tipo = 'SALIDA' AND UPPER(m.observacion) LIKE '%VENTA%' THEN m.cantidad ELSE 0 END),
    SUM(CASE WHEN m.tipo = 'PERDIDA' THEN m.cantidad ELSE 0 END),
    SUM(CASE WHEN m.tipo = 'SALIDA' AND (m.observacion IS NULL OR UPPER(m.observacion) NOT LIKE '%VENTA%') THEN m.cantidad ELSE 0 END),
    SUM(CASE WHEN m.tipo = 'ENTRADA' THEN m.cantidad ELSE 0 END)
FROM MovimientoInventario m
WHERE m.sede.id = :sedeId
  AND m.fecha BETWEEN :inicio AND :fin
GROUP BY m.producto.id
""")
    List<Object[]> resumenMovimientosDelDia(
            @Param("sedeId") Long sedeId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin
    );
}
