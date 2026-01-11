package proyecto.dto;

public record ProductoMateriaPrimaRequestDTO(
        Long productoId,
        String productoNombre,
        Long materiaPrimaId,
        String materiaPrimaNombre,
        double mlConsumidos
) {}

