package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.entidades.MesaEstado;

import java.util.List;
import java.util.Optional;

@Repository
public interface MesaEstadoRepository extends JpaRepository<MesaEstado, Long> {

    @Query("""
        SELECT DISTINCT m
        FROM MesaEstado m
        LEFT JOIN FETCH m.items
        WHERE m.sede.id = :sedeId
        ORDER BY m.mesaReferenciaId ASC
    """)
    List<MesaEstado> findDetalleBySedeId(@Param("sedeId") Long sedeId);

    @Query("""
        SELECT DISTINCT m
        FROM MesaEstado m
        LEFT JOIN FETCH m.items
        WHERE m.sede.id = :sedeId
          AND m.mesaReferenciaId = :mesaId
    """)
    Optional<MesaEstado> findDetalleBySedeIdAndMesaReferenciaId(
            @Param("sedeId") Long sedeId,
            @Param("mesaId") Long mesaId
    );
}
