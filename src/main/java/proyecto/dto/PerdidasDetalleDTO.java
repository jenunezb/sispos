package proyecto.dto;

import java.time.LocalDateTime;

public record PerdidasDetalleDTO(
        LocalDateTime fecha,
        Integer cantidad,
        String observacion
) {
}
