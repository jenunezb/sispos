package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.entidades.ComandaCocina;
import proyecto.entidades.EstadoComandaCocina;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComandaCocinaRepository extends JpaRepository<ComandaCocina, Long> {

    @Query("""
        SELECT DISTINCT c
        FROM ComandaCocina c
        LEFT JOIN FETCH c.detalles
        LEFT JOIN FETCH c.sede s
        LEFT JOIN FETCH c.vendedor
        LEFT JOIN FETCH c.administrador
        WHERE s.empresa.nit = :empresaNit
          AND c.estado IN :estados
        ORDER BY c.fechaCreacion ASC
    """)
    List<ComandaCocina> findDetalleByEmpresaNitAndEstadoInOrderByFechaCreacionAsc(
            @Param("empresaNit") Long empresaNit,
            @Param("estados") Collection<EstadoComandaCocina> estados
    );

    @Query("""
        SELECT DISTINCT c
        FROM ComandaCocina c
        LEFT JOIN FETCH c.detalles
        LEFT JOIN FETCH c.sede s
        LEFT JOIN FETCH c.vendedor
        LEFT JOIN FETCH c.administrador
        WHERE s.empresa.nit = :empresaNit
          AND c.id = :comandaId
    """)
    Optional<ComandaCocina> findDetalleByEmpresaNitAndId(
            @Param("empresaNit") Long empresaNit,
            @Param("comandaId") Long comandaId
    );
}
