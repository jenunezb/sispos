package proyecto.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoVendedorDTO(
        @NotNull(message = "El código del vendedor es obligatorio")
        Long codigo,

        @NotNull(message = "El estado es obligatorio")
        Boolean estado
) {
}
