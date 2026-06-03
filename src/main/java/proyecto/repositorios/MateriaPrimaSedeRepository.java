package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import proyecto.entidades.MateriaPrima;
import proyecto.entidades.MateriaPrimaSede;
import proyecto.entidades.Sede;

import java.util.List;
import java.util.Optional;

public interface MateriaPrimaSedeRepository extends JpaRepository<MateriaPrimaSede, Long> {

    // 🔒 Para validar
    boolean existsByMateriaPrimaAndSede(
            MateriaPrima materiaPrima,
            Sede sede
    );

    // 🔍 Para obtener
    Optional<MateriaPrimaSede> findByMateriaPrimaAndSede(
            MateriaPrima materiaPrima,
            Sede sede
    );


    Optional<MateriaPrimaSede> findByMateriaPrimaCodigoAndSedeId(
            Long materiaPrimaCodigo,
            Long sedeId
    );

    boolean existsByMateriaPrimaAndSedeId(MateriaPrima materiaPrima, Long sedeId);

    List<MateriaPrimaSede> findBySedeIdOrderByIdAsc(Long sedeId);

    @Query("""
        SELECT mps
        FROM MateriaPrimaSede mps
        JOIN mps.materiaPrima mp
        WHERE mps.sede.id = :sedeId
          AND mps.activa = true
          AND mp.activa = true
        ORDER BY mp.nombre ASC
    """)
    List<MateriaPrimaSede> findActivasBySedeIdOrderByMateriaPrimaNombreAsc(@Param("sedeId") Long sedeId);

}



