package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Venta;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    boolean existsByVendedorCodigo(Long vendedorId);

    List<Venta> findByVendedorCodigoAndAnuladoFalse(Long vendedorId);

    List<Venta> findByVendedorCodigoAndAnuladoFalseAndFechaBetween(
            Long vendedorId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    List<Venta> findByVendedorCorreoAndAnuladoFalseOrderByFechaDesc(String correo);

    List<Venta> findByVendedorCorreoAndAnuladoFalseAndFechaBetweenOrderByFechaDesc(
            String correo,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    @Query("""
        SELECT v
        FROM Venta v
        LEFT JOIN v.vendedor vend
        WHERE v.sede.id = :sedeId
          AND v.anulado = false
          AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
    """)
    List<Venta> findBySedeId(@Param("sedeId") Long sedeId);

    @Query("""
        SELECT v
        FROM Venta v
        LEFT JOIN v.vendedor vend
        WHERE v.sede.id = :sedeId
          AND v.fecha BETWEEN :desde AND :hasta
          AND v.anulado = false
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
        WHERE v.anulado = false
    """)
    Double totalVentas();

    @Query("""
        SELECT COALESCE(SUM(v.total), 0)
        FROM Venta v
        LEFT JOIN v.vendedor vend
        WHERE v.sede.id = :sedeId
          AND v.anulado = false
          AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
    """)
    Double totalVentasPorSede(@Param("sedeId") Long sedeId);

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.fecha BETWEEN :desde AND :hasta
      AND v.anulado = false
""")
    Double totalVentasEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
    WHERE v.sede.empresa.nit = :empresaNit
      AND v.fecha BETWEEN :desde AND :hasta
      AND v.anulado = false
""")
    Double totalVentasEntreFechasPorEmpresa(
            @Param("empresaNit") Long empresaNit,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
    WHERE v.sede.empresa.nit = :empresaNit
      AND v.fecha BETWEEN :desde AND :hasta
      AND v.anulado = false
""")
    Long cantidadVentasEntreFechasPorEmpresa(
            @Param("empresaNit") Long empresaNit,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
    WHERE v.sede.empresa.nit = :empresaNit
      AND v.modoPago = 'EFECTIVO'
      AND v.fecha BETWEEN :desde AND :hasta
      AND v.anulado = false
""")
    Double totalVentasEntreFechasEfectivoPorEmpresa(
            @Param("empresaNit") Long empresaNit,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COALESCE(SUM(v.total), 0)
    FROM Venta v
    WHERE v.sede.empresa.nit = :empresaNit
      AND v.modoPago = 'TRANSFERENCIA'
      AND v.fecha BETWEEN :desde AND :hasta
      AND v.anulado = false
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
      AND v.anulado = false
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
      AND v.anulado = false
""")
    Long cantidadVentasEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.anulado = false
""")
    Long cantidadVentasTotal();

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.sede.id = :sedeId
      AND v.anulado = false
      AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
""")
    Long cantidadVentasPorSede(@Param("sedeId") Long sedeId);

    @Query("""
    SELECT COUNT(v)
    FROM Venta v
        LEFT JOIN v.vendedor vend
    WHERE v.sede.id = :sedeId
      AND v.fecha BETWEEN :desde AND :hasta
      AND v.anulado = false
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
      AND v.anulado = false
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
      AND v.anulado = false
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

    @Query("""
        SELECT v
        FROM Venta v
        LEFT JOIN v.vendedor vend
        WHERE v.sede.id = :sedeId
          AND v.anulado = true
          AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
    """)
    List<Venta> findBySedeIdAndAnuladoTrue(@Param("sedeId") Long sedeId);

    @Query("""
        SELECT v
        FROM Venta v
        LEFT JOIN v.vendedor vend
        WHERE v.sede.id = :sedeId
          AND v.fecha BETWEEN :desde AND :hasta
          AND v.anulado = true
          AND (vend IS NULL OR vend.tipoPerfil IS NULL OR vend.tipoPerfil <> proyecto.entidades.TipoPerfilVendedor.PRODUCCION)
    """)
    List<Venta> findBySedeIdAndFechaBetweenAndAnuladoTrue(
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
        SELECT v
        FROM Venta v
        WHERE (:empresaNit IS NULL OR v.sede.empresa.nit = :empresaNit)
          AND (:sedeId IS NULL OR v.sede.id = :sedeId)
          AND (:desde IS NULL OR v.fecha >= :desde)
          AND (:hasta IS NULL OR v.fecha <= :hasta)
        ORDER BY v.fecha DESC
    """)
    List<Venta> buscarVentasSistema(
            @Param("empresaNit") Long empresaNit,
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
        SELECT DISTINCT v
        FROM Venta v
        LEFT JOIN FETCH v.detalles d
        LEFT JOIN FETCH d.producto
        LEFT JOIN FETCH v.sede s
        LEFT JOIN FETCH s.empresa
        LEFT JOIN FETCH v.vendedor
        LEFT JOIN FETCH v.administrador
        LEFT JOIN FETCH v.cliente
        WHERE v.id = :ventaId
    """)
    Optional<Venta> findDetalleById(@Param("ventaId") Long ventaId);

    @Query("""
        SELECT DISTINCT v
        FROM Venta v
        LEFT JOIN FETCH v.detalles d
        LEFT JOIN FETCH d.producto
        LEFT JOIN FETCH v.sede s
        LEFT JOIN FETCH s.empresa
        LEFT JOIN FETCH v.vendedor
        LEFT JOIN FETCH v.administrador
        LEFT JOIN FETCH v.cliente
        ORDER BY v.fecha DESC
    """)
    List<Venta> findAllConDetalleParaSuperAdmin();

    @Query("""
        SELECT DISTINCT v
        FROM Venta v
        LEFT JOIN FETCH v.detalles d
        LEFT JOIN FETCH d.producto
        LEFT JOIN FETCH v.sede s
        LEFT JOIN FETCH s.empresa
        LEFT JOIN FETCH v.vendedor
        LEFT JOIN FETCH v.administrador
        LEFT JOIN FETCH v.cliente
        WHERE v.id = :ventaId
    """)
    Optional<Venta> findDetalleByIdParaSuperAdmin(@Param("ventaId") Long ventaId);

    Optional<Venta> findByIdAndSedeEmpresaNit(Long id, Long empresaNit);

    @Query("""
        SELECT COALESCE(MAX(v.numeroConsecutivo), 0)
        FROM Venta v
        WHERE v.sede.id = :sedeId
    """)
    Long findMaxNumeroConsecutivoBySedeId(@Param("sedeId") Long sedeId);

}
