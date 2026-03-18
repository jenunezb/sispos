package proyecto.dto;

import java.time.LocalDateTime;
import java.util.List;

public record VentaSeguimientoDTO(
        Long id,
        LocalDateTime fecha,
        Double total,
        Boolean anulado,
        Long empresaNit,
        String empresaNombre,
        Long sedeId,
        String sedeUbicacion,
        String usuarioNombre,
        String usuarioCorreo,
        Long clienteId,
        String clienteNombre,
        List<DetalleVentaResponseDTO> detalles
) {
}
