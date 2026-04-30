package proyecto.dto;

import java.time.LocalDate;

public record SedeDTO(
        Long id,
        String ubicacion,
        Boolean activa,
        LocalDate fechaProximoVencimiento
) {
}
