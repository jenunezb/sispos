package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Sede;

import java.util.List;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Long> {
    boolean existsByUbicacionIgnoreCase(String ubicacion);

    List<Sede> findByEmpresaNit(Long empresaNit);
}
