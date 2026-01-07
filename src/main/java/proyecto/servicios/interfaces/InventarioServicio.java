package proyecto.servicios.interfaces;

import proyecto.dto.InventarioDTO;
import proyecto.dto.MovimientoInventarioDTO;
import proyecto.dto.PerdidasDetalleDTO;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface InventarioServicio {

    List<InventarioDTO> listarPorSede(Long sedeId);

    List<InventarioDTO> listarPorSede1(Long sedeId);

    InventarioDTO obtenerPorProductoYSede(Long productoId, Long sedeId);

    void registrarEntrada(Long productoId, Long sedeId, Integer cantidad);

    void registrarSalida(Long productoId, Long sedeId, Integer cantidad, String observacion);

    void registrarPerdida(Long productoId, Long sedeId, Integer cantidad);

    void registrarMovimiento(MovimientoInventarioDTO dto);

    List<PerdidasDetalleDTO> obtenerPerdidasDetalladasPorRango(
            Long sedeId,
            LocalDateTime inicio,
            LocalDateTime fin
    );

}
