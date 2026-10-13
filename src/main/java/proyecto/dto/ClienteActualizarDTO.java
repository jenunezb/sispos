package proyecto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonAlias;

public record ClienteActualizarDTO(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 120) String nombre,
        @Size(max = 30) String telefono,
        @JsonAlias("nit") @Size(max = 30) String documento,
        @Email(message = "El correo del cliente no es valido") @Size(max = 254) String correo,
        @Size(max = 255) String direccion
) {
    public ClienteActualizarDTO(String nombre, String telefono, String documento) {
        this(nombre, telefono, documento, null, null);
    }
}
