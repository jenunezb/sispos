package proyecto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompraDTO(@NotNull Long sedeId, @NotNull Long proveedorId,
        @NotBlank @Size(max=80) String numeroFactura, @NotNull LocalDate fechaFactura,
        @NotBlank String condicionPago, String modoPago, LocalDate fechaVencimiento,
        @NotNull @DecimalMin("0.00") BigDecimal pagoInicial, String referenciaPago,
        @Size(max=500) String observacion, @NotEmpty List<@Valid Linea> items) {
    public record Linea(@NotBlank String tipo, Long materiaPrimaId, Long productoId,
        @NotNull @DecimalMin(value="0.001") BigDecimal cantidad,
        @NotNull @DecimalMin(value="0.00") BigDecimal costoUnitario,
        @Size(max=20) String unidadBase, @Size(max=80) String presentacion,
        BigDecimal contenidoPresentacion) {}
}
