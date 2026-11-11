package proyecto.dto;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoVentaDTO(
        @NotNull(message = "El id de la venta es obligatorio")
        Long ventaId,

        @NotNull(message = "El estado válido/inválido es obligatorio")
        Boolean valido
) {
}
