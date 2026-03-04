package proyecto.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PrecioClienteRequestDTO(
        @NotNull(message = "El código del producto es obligatorio")
        Long productoCodigo,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor a 0")
        Double precioVenta
) {
}
