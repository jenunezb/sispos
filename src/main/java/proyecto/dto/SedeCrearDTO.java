package proyecto.dto;

import jakarta.validation.constraints.NotBlank;

public record SedeCrearDTO(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "La ubicación es obligatoria")
        String ubicacion
) {
}
