package proyecto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductoCrearDTO(@NotBlank(message = "El nombre es obligatorio")
                                String nombre,

                               @NotBlank(message = "La descripción es obligatoria")
                               String descripcion,

                               @NotNull(message = "El precio de producción es obligatorio")
                                @Positive(message = "El precio de producción debe ser mayor a 0")
                                Double precioProduccion,

                               @NotNull(message = "El precio de venta es obligatorio")
                                @Positive(message = "El precio de venta debe ser mayor a 0")
                                Double precioVenta,

                               @NotNull(message = "La cantidad es obligatoria")
                                @Min(value = 0, message = "La cantidad no puede ser negativa")
                                Integer cantidad,

                               String categoria) {
}