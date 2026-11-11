package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.entidades.GastoDiario;
import proyecto.entidades.ModoPago;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GastoDiarioRepository extends JpaRepository<GastoDiario, Long> {

    @Query("""
            SELECT COALESCE(SUM(g.valor), 0)
            FROM GastoDiario g
            WHERE g.sede.empresa.nit = :empresaNit
              AND g.fecha BETWEEN :desde AND :hasta
            """)
    Double totalGastosPorEmpresa(
            @Param("empresaNit") Long empresaNit,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
            SELECT COALESCE(SUM(g.valor), 0)
            FROM GastoDiario g
            WHERE g.sede.empresa.nit = :empresaNit
              AND g.modoPago = :modoPago
              AND g.fecha BETWEEN :desde AND :hasta
            """)
    Double totalGastosPorEmpresaYModoPago(
            @Param("empresaNit") Long empresaNit,
            @Param("modoPago") ModoPago modoPago,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
            SELECT COALESCE(SUM(g.valor), 0)
            FROM GastoDiario g
            WHERE g.sede.id = :sedeId
              AND g.fecha BETWEEN :desde AND :hasta
            """)
    Double totalGastosPorSede(
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
            SELECT COALESCE(SUM(g.valor), 0)
            FROM GastoDiario g
            WHERE g.sede.id = :sedeId
              AND g.modoPago = :modoPago
              AND g.fecha BETWEEN :desde AND :hasta
            """)
    Double totalGastosPorSedeYModoPago(
            @Param("sedeId") Long sedeId,
            @Param("modoPago") ModoPago modoPago,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
            SELECT g
            FROM GastoDiario g
            WHERE (:sedeId IS NULL OR g.sede.id = :sedeId)
              AND g.sede.empresa.nit = :empresaNit
              AND g.fecha BETWEEN :desde AND :hasta
            ORDER BY g.fecha DESC, g.id DESC
            """)
    List<GastoDiario> listarPorEmpresaYSedeEntreFechas(
            @Param("empresaNit") Long empresaNit,
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}
