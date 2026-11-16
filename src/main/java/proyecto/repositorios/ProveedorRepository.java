package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Proveedor;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    java.util.List<Proveedor> findByEmpresaNitAndActivoTrueOrderByNombreAsc(Long empresaNit);
    java.util.Optional<Proveedor> findByIdAndEmpresaNit(Long id, Long empresaNit);
    java.util.Optional<Proveedor> findByIdAndEmpresaNitAndActivoTrue(Long id, Long empresaNit);
}
