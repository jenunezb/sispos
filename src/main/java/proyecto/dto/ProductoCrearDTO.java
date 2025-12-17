package proyecto.dto;

import jakarta.validation.constraints.*;

public record ProductoCrearDTO(@NotBlank(message = "El nombre es obligatorio")
                                String nombre,

                                String descripcion,

                               @PositiveOrZero(message = "El precio de producción debe ser mayor o igual a 0")
                               Double precioProduccion,

                               @NotNull(message = "El precio de venta es obligatorio")
                                @Positive(message = "El precio de venta debe ser mayor a 0")
                                Double precioVenta,

                               String categoria) {
}