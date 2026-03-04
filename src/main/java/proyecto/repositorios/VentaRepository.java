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

    List<Venta> findByVendedorCorreoOrderByFechaDesc(String correo);

    List<Venta> findByVendedorCorreoAndFechaBetweenOrderByFechaDesc(
            String correo,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    @Query("""
        SELECT v
        FROM Venta v
        LEFT JOIN v.vendedor vend
        WHERE v.sede.id = :sedeId
          AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
    """)
    List<Venta> findBySedeId(@Param("sedeId") Long sedeId);

    @Query("""
        SELECT v
        FROM Venta v
        LEFT JOIN v.vendedor vend
        WHERE v.sede.id = :sedeId
          AND v.fecha BETWEEN :desde AND :hasta
          AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
    """)
    List<Venta> findBySedeIdAndFechaBetween(
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
        SELECT COALESCE(SUM(v.total), 0)
        FROM Venta v
        LEFT JOIN v.vendedor vend
    """)
    Double totalVentas();

    @Query("""
        SELECT COALESCE(SUM(v.total), 0)
        FROM Venta v
        LEFT JOIN v.vendedor vend
        WHERE v.sede.id = :sedeId
          AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
    """)
    Double totalVentasPorSede(@Param("sedeId") Long sedeId);

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.fecha BETWEEN :desde AND :hasta
""")
    Double totalVentasEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.sede.empresa.nit = :empresaNit
      AND v.fecha BETWEEN :desde AND :hasta
      AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
""")
    Double totalVentasEntreFechasPorEmpresa(
            @Param("empresaNit") Long empresaNit,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.sede.empresa.nit = :empresaNit
      AND v.fecha BETWEEN :desde AND :hasta
      AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
""")
    Long cantidadVentasEntreFechasPorEmpresa(
            @Param("empresaNit") Long empresaNit,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.sede.empresa.nit = :empresaNit
      AND v.modoPago = 'EFECTIVO'
      AND v.fecha BETWEEN :desde AND :hasta
      AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
""")
    Double totalVentasEntreFechasEfectivoPorEmpresa(
            @Param("empresaNit") Long empresaNit,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.sede.empresa.nit = :empresaNit
      AND v.modoPago = 'TRANSFERENCIA'
      AND v.fecha BETWEEN :desde AND :hasta
      AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
""")
    Double totalVentasEntreFechasTransferenciaPorEmpresa(
            @Param("empresaNit") Long empresaNit,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.sede.id = :sedeId
      AND v.fecha BETWEEN :desde AND :hasta
      AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
""")
    Double totalVentasPorSedeEntreFechas(
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.fecha BETWEEN :desde AND :hasta
""")
    Long cantidadVentasEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
        LEFT JOIN v.vendedor vend
""")
    Long cantidadVentasTotal();

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.sede.id = :sedeId
      AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
""")
    Long cantidadVentasPorSede(@Param("sedeId") Long sedeId);

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.sede.id = :sedeId
      AND v.fecha BETWEEN :desde AND :hasta
      AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
""")
    Long cantidadVentasPorSedeEntreFechas(
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.modoPago = 'EFECTIVO'
      AND v.fecha BETWEEN :desde AND :hasta
""")
    Double totalVentasEntreFechasEfectivo(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.modoPago = 'TRANSFERENCIA'
      AND v.fecha BETWEEN :desde AND :hasta
""")
    Double totalVentasEntreFechasTransferencia(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.sede.id = :sedeId
      AND v.fecha BETWEEN :desde AND :hasta
      AND v.modoPago = proyecto.entidades.ModoPago.EFECTIVO
      AND v.anulado = false
      AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
""")
    Double totalVentasEfectivoPorSedeEntreFechas(
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.sede.id = :sedeId
      AND v.fecha BETWEEN :desde AND :hasta
      AND v.modoPago = proyecto.entidades.ModoPago.TRANSFERENCIA
      AND v.anulado = false
      AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
""")
    Double totalVentasTransferenciaPorSedeEntreFechas(
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    List<Venta> findByVendedorCodigoAndAnuladoFalse(Long vendedorId);

    @Query("""
        SELECT v
        FROM Venta v
        LEFT JOIN v.vendedor vend
        WHERE v.sede.id = :sedeId
          AND v.anulado = true
          AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
    """)
    List<Venta> findBySedeIdAndAnuladoTrue(@Param("sedeId") Long sedeId);

}


