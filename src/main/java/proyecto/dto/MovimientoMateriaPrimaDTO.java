package proyecto.dto;

public record MovimientoMateriaPrimaDTO(
        Long materiaPrimaId,
        Long sedeId,
        String tipo, // "ENTRADA" | "SALIDA"
        Double cantidad
) {}

