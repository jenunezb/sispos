package proyecto.dto;

import java.time.LocalDateTime;

public record PerdidasDetalleDTO(
        LocalDateTime fecha,
        String producto,
        Integer cantidad,
        String observacion
) {
}
