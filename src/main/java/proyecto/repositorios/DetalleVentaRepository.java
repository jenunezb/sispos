package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.entidades.DetalleVenta;

import java.time.LocalDateTime;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
    @Query("""
        SELECT COALESCE(SUM(d.cantidad * p.precioProduccion), 0)
        FROM DetalleVenta d
        JOIN d.producto p
        JOIN d.venta v
    """)
    Double costoProduccionTotal();

    @Query("""
        SELECT COALESCE(SUM(d.cantidad * p.precioProduccion), 0)
        FROM DetalleVenta d
        JOIN d.producto p
        JOIN d.venta v
        WHERE v.sede.id = :sedeId
    """)
    Double costoProduccionPorSede(@Param("sedeId") Long sedeId);

    @Query("""
    SELECT COALESCE(SUM(d.cantidad * p.precioProduccion), 0)
    FROM DetalleVenta d
    JOIN d.producto p
    JOIN d.venta v
    WHERE v.fecha BETWEEN :desde AND :hasta
""")
    Double costoProduccionEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(d.cantidad * p.precioProduccion), 0)
    FROM DetalleVenta d
    JOIN d.producto p
    JOIN d.venta v
    WHERE v.sede.id = :sedeId
    AND v.fecha BETWEEN :desde AND :hasta
""")
    Double costoProduccionPorSedeEntreFechas(
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

}
