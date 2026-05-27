package proyecto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductoProduccionRequestDTO(
        @NotBlank(message = "El nombre del producto es obligatorio")
        String nombre,

        @NotNull(message = "El precio base es obligatorio")
        @Positive(message = "El precio base debe ser mayor a cero")
        Double precioBase
) {
}
