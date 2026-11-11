package proyecto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import proyecto.entidades.ModoPago;

public record CorregirPagoDTO(@NotNull ModoPago modoPagoAnterior,
                             @NotNull ModoPago modoPago,
                             @NotBlank @Size(max = 500) String motivo) {}
