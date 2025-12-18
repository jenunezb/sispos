package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import proyecto.entidades.Venta;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findByVendedorCodigo(Long vendedorId);

    List<Venta> findByVendedorCodigoAndFechaBetween(
            Long vendedorId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    List<Venta> findBySedeId(Long sedeId);

    List<Venta> findBySedeIdAndFechaBetween(
            Long sedeId,
            LocalDateTime desde,
            LocalDateTime hasta
    );
}