package proyecto.dto;

import java.time.LocalDate;
import java.util.List;

public record InformeProduccionDiaDTO(
        LocalDate fecha,
        Integer totalProducido,
        Integer totalDespachado,
        List<ResumenProductoProduccionDTO> productos,
        List<DespachoClienteProduccionDTO> despachosPorCliente
) {
}
