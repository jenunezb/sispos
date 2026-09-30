package proyecto.dto;

import java.time.LocalDate;

public record SedeDTO(
        Long id,
        String ubicacion,
        String estadoSuscripcion,
        LocalDate fechaVencimientoSuscripcion
) {
}
