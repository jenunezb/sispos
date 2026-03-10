package proyecto.dto;

import jakarta.validation.constraints.NotBlank;

public record ClienteCrearDTO(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        String nombre,
        String telefono,
        String documento
) {
}
