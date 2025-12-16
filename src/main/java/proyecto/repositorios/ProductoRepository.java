package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Producto;

import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    // Puedes agregar consultas personalizadas si lo necesitas
    // Ejemplo: List<Producto> findByNombreContainingIgnoreCase(String nombre);

    // Buscar productos activos
    List<Producto> findByEstadoTrue();

    // Buscar por nombre (opcional, útil más adelante)
    List<Producto> findByNombreContainingIgnoreCase(String nombre);
}
