package proyecto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AdministradorEmpresaCrearDTO(
        @Email(message = "Debe ingresar un correo valido")
        @NotBlank(message = "El correo es obligatorio")
        String correo,

        @NotBlank(message = "La password es obligatoria")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        String apellido,

        @NotNull(message = "El celular es obligatorio")
        Long celular,

        @NotEmpty(message = "Debe asignar al menos una sede")
        List<Long> sedeIds
) {
}
