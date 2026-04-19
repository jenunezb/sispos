package proyecto.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VentaResponseDTO(
        Long id,
        Long consecutivo,
        LocalDateTime fecha,
        Double total,
        String modoPago,
        String vendedorNombre,
        String sedeUbicacion,
        Long clienteId,
        String clienteNombre,
        Boolean anulado,
        Boolean valido,
        List<DetalleVentaResponseDTO> detalles
) {
}
