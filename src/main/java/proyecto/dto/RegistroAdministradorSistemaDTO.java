package proyecto.dto;

public record RegistroAdministradorSistemaDTO(
        String correo,
        String password,
        String nombre,
        String apellido,
        Long celular
) {
}
