package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.SalidaEspecial;

@Repository
public interface SalidaEspecialRepository extends JpaRepository<SalidaEspecial, Long> {
}
