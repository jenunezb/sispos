package proyecto.dto;

import java.time.LocalDateTime;

public record ProduccionMovimientoManualDTO(
        Long id,
        Long productoId,
        String productoNombre,
        Integer stockAnterior,
        Integer stockNuevo,
        Integer diferencia,
        Long usuarioId,
        String usuarioNombre,
        String usuarioCorreo,
        LocalDateTime fecha,
        String observacion
) {
}
