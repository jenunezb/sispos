package proyecto.dto;

/**
 * DTO para actualizar cantidad, ml por vaso y estado
 */
public record MateriaPrimaSedeUpdate(
        double cantidad,
        double mlPorVaso,
        boolean activa
) {}
