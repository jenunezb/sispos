package proyecto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SedeActualizarDTO(
        @NotNull(message = "El id es obligatorio")
        Long id,

        @NotBlank
        String nombre,

        @NotBlank
        String ubicacion
) {
}
