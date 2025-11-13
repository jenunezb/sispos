package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Administrador;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
}
