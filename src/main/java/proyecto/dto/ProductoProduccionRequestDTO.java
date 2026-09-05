package proyecto.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductoProduccionRequestDTO(
        @NotBlank(message = "El nombre del producto es obligatorio")
        String nombre,

        String descripcion,

        @JsonAlias({"precioVenta"})
        @PositiveOrZero(message = "El precio base debe ser mayor o igual a cero")
        Double precioBase,

        @PositiveOrZero(message = "El precio de producción debe ser mayor o igual a cero")
        Double precioProduccion,

        @JsonAlias({"precioBase"})
        @PositiveOrZero(message = "El precio de venta debe ser mayor o igual a cero")
        Double precioVenta
) {
}
