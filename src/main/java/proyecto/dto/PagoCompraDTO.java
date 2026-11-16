package proyecto.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record PagoCompraDTO(@NotNull @DecimalMin(value="0.01") BigDecimal monto, @NotBlank String modoPago,
        @NotNull LocalDate fecha, @Size(max=255) String referencia) {}
