package proyecto.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProduccionRegistroDTO(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor a cero")
        Integer cantidad,

        String observacion
) {
}
