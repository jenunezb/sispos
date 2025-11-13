package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    // Puedes agregar consultas personalizadas si lo necesitas
    // Ejemplo: List<Producto> findByNombreContainingIgnoreCase(String nombre);
}
