package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.PrecioClienteProducto;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrecioClienteProductoRepository extends JpaRepository<PrecioClienteProducto, Long> {

    Optional<PrecioClienteProducto> findByClienteIdAndProductoCodigo(Long clienteId, Long productoCodigo);

    List<PrecioClienteProducto> findByClienteIdAndClienteEmpresaNitAndActivoTrueOrderByProductoNombreAsc(Long clienteId, Long empresaNit);
}
