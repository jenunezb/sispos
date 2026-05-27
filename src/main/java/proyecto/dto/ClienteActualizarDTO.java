package proyecto.dto;

import jakarta.validation.constraints.NotBlank;

public record ClienteActualizarDTO(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        String nombre,
        String telefono,
        String documento
) {
}
