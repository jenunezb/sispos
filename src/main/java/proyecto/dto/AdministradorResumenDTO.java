package proyecto.dto;

public record AdministradorResumenDTO(
        Integer codigo,
        String nombre,
        String apellido,
        String correo,
        Long celular,
        Long empresaNit,
        String empresaNombre,
        boolean superAdmin
) {
}
