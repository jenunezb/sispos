package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.PagoProveedor;

@Repository
public interface PagoProveedorRepository extends JpaRepository<PagoProveedor, Long> {
}