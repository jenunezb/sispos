package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Sede;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Long> {
}
