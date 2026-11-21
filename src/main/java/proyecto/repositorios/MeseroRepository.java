package proyecto.repositorios;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import proyecto.entidades.Mesero;

import java.util.List;
import java.util.Optional;

public interface MeseroRepository extends JpaRepository<Mesero, Long> {
    Optional<Mesero> findByUsuario(String usuario);
    @EntityGraph(attributePaths = "sede")
    Optional<Mesero> findByIdAndActivoTrue(Long id);
    Optional<Mesero> findByIdAndSedeId(Long id, Long sedeId);
    List<Mesero> findBySedeIdOrderByNombreAsc(Long sedeId);
    boolean existsByUsuario(String usuario);
}
