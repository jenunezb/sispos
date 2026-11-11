package proyecto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import proyecto.entidades.ModoPago;

public record GastoDiarioCrearDTO(
        @NotNull(message = "La sede es obligatoria")
        Long sedeId,

        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion,

        @NotNull(message = "El valor es obligatorio")
        @Positive(message = "El valor debe ser mayor a 0")
        Double valor,

        @NotNull(message = "El modo de pago es obligatorio")
        ModoPago modoPago
) {
}
