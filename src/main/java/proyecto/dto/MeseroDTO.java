package proyecto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public final class MeseroDTO {
    private MeseroDTO() {}

    public record Alta(
            @NotBlank(message = "Ingresa el nombre del mesero")
            @Size(max = 120, message = "El nombre admite hasta 120 caracteres") String nombre,
            @NotBlank(message = "Ingresa un nombre de usuario")
            @Pattern(regexp = "[a-zA-Z0-9._-]{3,40}",
                    message = "El usuario debe tener de 3 a 40 letras, números, puntos, guiones o guiones bajos") String usuario,
            @NotBlank(message = "Ingresa una contraseña")
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password
    ) {}

    public record Edicion(
            @NotBlank(message = "Ingresa el nombre del mesero")
            @Size(max = 120, message = "El nombre admite hasta 120 caracteres") String nombre,
            @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres") String password,
            @NotNull(message = "Indica si la cuenta está activa") Boolean activo
    ) {}

    public record Cuenta(Long id, String usuario, String nombre, boolean activo) {}

    public record Login(
            @NotBlank(message = "Ingresa tu usuario")
            @Size(max = 40, message = "Revisa tu usuario") String usuario,
            @NotBlank(message = "Ingresa tu contraseña")
            @Size(max = 72, message = "Revisa tu contraseña") String password
    ) {}

    public record Sesion(String token, String nombre, String sede, boolean cocinaHabilitada) {}

    public record Producto(Long id, String nombre, Double precio) {}

    public record Renglon(String nombre, Integer cantidad, Double total) {}

    public record Mesa(Long id, String nombre, String estado, Long version, List<Renglon> pedido) {}

    public record Linea(@NotNull @Positive Long productoId, @NotNull @Min(1) @Max(999) Integer cantidad) {}

    public record Pedido(
            @NotNull @PositiveOrZero Long version,
            @NotEmpty @Size(max = 100) List<@NotNull @Valid Linea> productos,
            boolean enviarCocina,
            @Size(max = 1000) String observaciones
    ) {}

    public record PedidoGuardado(Mesa mesa, Long comandaId) {}
}
