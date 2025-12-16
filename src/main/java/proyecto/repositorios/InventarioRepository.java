package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Inventario;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {
    // Listar todo el inventario de una sede
    List<Inventario> findBySedeId(Long sedeId);

    // Obtener inventario de un producto específico en una sede
    Optional<Inventario> findByProductoCodigoAndSedeId(Long productoId, Long sedeId);

    // Verificar si existe inventario para producto + sede
    boolean existsByProductoCodigoAndSedeId(Long productoId, Long sedeId);
}
