package proyecto.dto;

public record MateriaPrimaSedeDTO(
        Long id,
        Long materiaPrimaId,
        String nombreMateriaPrima,
        boolean materiaPrimaActiva,

        Long sedeId,
        String nombreSede,

        double cantidadActualMl,
        double mlPorVaso,
        boolean activa
) {
}
