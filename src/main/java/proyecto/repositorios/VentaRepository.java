package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Venta;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByVendedorCodigo(Long vendedorId);

    List<Venta> findByVendedorCodigoAndFechaBetween(
            Long vendedorId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    List<Venta> findBySedeId(Long sedeId);

    List<Venta> findBySedeIdAndFechaBetween(
            Long sedeId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    @Query("""
        SELECT COALESCE(SUM(v.total), 0)
        FROM Venta v
    """)
    Double totalVentas();

    @Query("""
        SELECT COALESCE(SUM(v.total), 0)
        FROM Venta v
        WHERE v.sede.id = :sedeId
    """)
    Double totalVentasPorSede(@Param("sedeId") Long sedeId);

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
    WHERE v.fecha BETWEEN :desde AND :hasta
""")
    Double totalVentasEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
    WHERE v.sede.id = :sedeId
    AND v.fecha BETWEEN :desde AND :hasta
""")
    Double totalVentasPorSedeEntreFechas(
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
    WHERE v.fecha BETWEEN :desde AND :hasta
""")
    Long cantidadVentasEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
""")
    Long cantidadVentasTotal();

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
    WHERE v.sede.id = :sedeId
""")
    Long cantidadVentasPorSede(@Param("sedeId") Long sedeId);

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
    WHERE v.sede.id = :sedeId
      AND v.fecha BETWEEN :desde AND :hasta
""")
    Long cantidadVentasPorSedeEntreFechas(
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

}