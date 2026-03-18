package proyecto.dto;

public record SuperAdminUsuarioDTO(
        String tipoUsuario,
        String perfil,
        Integer codigo,
        String nombre,
        String apellido,
        String correo,
        String cedula,
        String telefono,
        Long celular,
        String ciudad,
        Boolean estado,
        Long empresaNit,
        String empresaNombre,
        Long sedeId,
        String sedeUbicacion,
        Boolean superAdmin
) {
}
