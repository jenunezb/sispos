package proyecto.dto;

import jakarta.validation.constraints.*;

public record ProductoActualizarDTO(@NotNull(message = "El código del producto es obligatorio")
                                     Long codigo,

                                    @NotBlank(message = "El nombre es obligatorio")
                                     String nombre,

                                    String descripcion,

                                     @PositiveOrZero
                                     Double precioProduccion,

                                    @NotNull
                                     @Positive
                                     Double precioVenta,

                                    String categoria,

                                    Boolean estado,
                                    @Size(max = 100, message = "El código de barras admite máximo 100 caracteres")
                                    String codigoBarras) {
}
