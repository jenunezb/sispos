package proyecto.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VentaResponseDTO(
        Long id,
        LocalDateTime fecha,
        Double total,
        String vendedorNombre,
        String sedeNombre,
        List<DetalleVentaResponseDTO> detalles
) {
}
