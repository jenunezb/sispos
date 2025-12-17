package proyecto.dto;

import proyecto.entidades.TipoMovimiento;

public record MovimientoInventarioDTO (Long sedeId,
                                       Long productoId,
                                       TipoMovimiento tipo,
                                       Integer cantidad,
                                       String observacion){

}
