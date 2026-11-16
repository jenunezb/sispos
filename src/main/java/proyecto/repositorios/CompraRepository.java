package proyecto.repositorios;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import proyecto.entidades.Compra;
import java.time.LocalDate;
import java.util.List;
public interface CompraRepository extends JpaRepository<Compra,Long> {
 boolean existsBySedeIdAndProveedorIdAndNumeroFacturaIgnoreCase(Long sedeId, Long proveedorId, String numeroFactura);
 @Query("select c from Compra c where (:sedeId is null or c.sede.id=:sedeId) and (:desde is null or c.fechaFactura>=:desde) and (:hasta is null or c.fechaFactura<=:hasta) order by c.fechaRecepcion desc")
 List<Compra> buscar(@Param("sedeId") Long sedeId,@Param("desde") LocalDate desde,@Param("hasta") LocalDate hasta);
 @Query("select c from Compra c where c.sede.id in :sedeIds and (:desde is null or c.fechaFactura>=:desde) and (:hasta is null or c.fechaFactura<=:hasta) order by c.fechaRecepcion desc")
 List<Compra> buscarPorSedes(@Param("sedeIds") List<Long> sedeIds,@Param("desde") LocalDate desde,@Param("hasta") LocalDate hasta);
}
