package proyecto.dto;

public record SedeResumenDTO(
        Long id,
        String ubicacion,
        Long empresaNit,
        String empresaNombre,
        String administradorCorreo
) {
}
