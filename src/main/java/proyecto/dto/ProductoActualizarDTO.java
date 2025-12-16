package proyecto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductoActualizarDTO(@NotNull(message = "El código del producto es obligatorio")
                                     Long codigo,

                                    @NotBlank(message = "El nombre es obligatorio")
                                     String nombre,

                                    @NotBlank String descripcion,

                                     @Positive
                                     Double precioProduccion,

                                    @NotNull
                                     @Positive
                                     Double precioVenta,

                                    String categoria,

                                    Boolean estado) {
}
