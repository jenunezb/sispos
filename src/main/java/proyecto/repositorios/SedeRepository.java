package proyecto.repositorios;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Sede;

import java.util.List;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Long> {
    boolean existsByUbicacionIgnoreCase(String ubicacion);

    List<Sede> findByEmpresaNit(Long empresaNit);

    @Query("""
            SELECT DISTINCT s
            FROM Sede s
            JOIN s.administradoresAsignados admin
            WHERE admin.codigo = :administradorCodigo
            ORDER BY s.ubicacion ASC
            """)
    List<Sede> findByAdministradorAsignado(@Param("administradorCodigo") Integer administradorCodigo);

    List<Sede> findByEmpresaNitAndIdIn(Long empresaNit, List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Sede s WHERE s.id = :id")
    java.util.Optional<Sede> findByIdForUpdate(@Param("id") Long id);
}
