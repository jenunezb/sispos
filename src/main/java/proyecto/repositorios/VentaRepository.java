package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Venta;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    // Ejemplo: List<Venta> findByFechaBetween(LocalDate inicio, LocalDate fin);
}