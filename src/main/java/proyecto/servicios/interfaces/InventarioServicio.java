package proyecto.servicios.interfaces;

import proyecto.dto.*;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface InventarioServicio {

    List<InventarioDTO> listarPorSede(Long sedeId);

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

    List<InventarioDelDia> obtenerInventarioDia(
            Long sedeId,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    );

    public List<MateriaPrimaInventarioDTO> obtenerInventarioMateriaPrimaDia(
            Long sedeId,
            LocalDateTime inicio,
            LocalDateTime fin
    );
}
