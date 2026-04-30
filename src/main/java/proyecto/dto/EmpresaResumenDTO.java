package proyecto.dto;

public record EmpresaResumenDTO(
        Long nit,
        String nombre,
        int totalSedes,
        int totalAdministradores
) {
}
