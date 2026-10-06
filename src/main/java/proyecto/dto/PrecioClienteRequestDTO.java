package proyecto.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PrecioClienteRequestDTO(
        @NotNull(message = "El producto es obligatorio")
        @JsonAlias({"productoCodigo"})
        Long productoId,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor a 0")
        @JsonAlias({"precioVenta"})
        Double precio
) {
}
