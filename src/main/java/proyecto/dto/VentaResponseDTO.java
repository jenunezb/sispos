package proyecto.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VentaResponseDTO(
        Long id,
        LocalDateTime fecha,
        Double total,
        String vendedorNombre,
        String sedeUbicacion,
        Long clienteId,
        String clienteNombre,
        List<DetalleVentaResponseDTO> detalles
) {
}
