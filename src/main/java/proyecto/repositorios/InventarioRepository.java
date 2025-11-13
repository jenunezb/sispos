package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Inventario;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {
}
