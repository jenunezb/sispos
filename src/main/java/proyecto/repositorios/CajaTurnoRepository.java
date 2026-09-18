package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.entidades.CajaTurno;
import proyecto.entidades.EstadoCaja;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CajaTurnoRepository extends JpaRepository<CajaTurno, Long> {

    boolean existsBySedeIdAndEstado(Long sedeId, EstadoCaja estado);

    Optional<CajaTurno> findFirstBySedeIdAndEstadoOrderByFechaAperturaDesc(Long sedeId, EstadoCaja estado);

    @Query("""
            SELECT c
            FROM CajaTurno c
            WHERE c.sede.empresa.nit = :empresaNit
              AND (:sedeId IS NULL OR c.sede.id = :sedeId)
              AND c.fechaApertura BETWEEN :desde AND :hasta
            ORDER BY c.fechaApertura DESC, c.id DESC
            """)
    List<CajaTurno> listarPorEmpresaYSedeEntreFechas(
            @Param("empresaNit") Long empresaNit,
            @Param("sedeId") Long sedeId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}
