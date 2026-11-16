package proyecto.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import proyecto.entidades.MovimientoProduccion;
import proyecto.entidades.TipoMovimientoProduccion;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimientoProduccionRepository extends JpaRepository<MovimientoProduccion, Long> {

    boolean existsByVendedorCodigo(Long vendedorId);

    List<MovimientoProduccion> findBySedeIdAndFechaBetweenOrderByFechaAsc(Long sedeId, LocalDateTime inicio, LocalDateTime fin);

    List<MovimientoProduccion> findTop200BySedeIdAndTipoOrderByFechaDesc(
            Long sedeId,
            TipoMovimientoProduccion tipo
    );

    @Query("""
            select m.producto.codigo,
                   sum(case
                       when m.tipo = :produccion then m.cantidad
                       when m.tipo = :despacho then -m.cantidad
                       else m.cantidad
                   end)
            from MovimientoProduccion m
            where m.sede.id = :sedeId and m.fecha > :fecha
            group by m.producto.codigo
            """)
    List<Object[]> sumarVariacionStockPosterior(
            @Param("sedeId") Long sedeId,
            @Param("fecha") LocalDateTime fecha,
            @Param("produccion") TipoMovimientoProduccion produccion,
            @Param("despacho") TipoMovimientoProduccion despacho
    );
}
