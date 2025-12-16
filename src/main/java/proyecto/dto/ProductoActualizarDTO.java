package proyecto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductoActualizarDTO(@NotNull(message = "El código del producto es obligatorio")
                                     Long codigo,

                                    @NotBlank(message = "El nombre es obligatorio")
                                     String nombre,

                                    @NotNull
                                     @Positive
                                     Double precioProduccion,

                                    @NotNull
                                     @Positive
                                     Double precioVenta,

                                    @NotNull
                                     @Min(0)
                                     Integer cantidad,

                                    String categoria,

                                    Boolean estado) {
}
