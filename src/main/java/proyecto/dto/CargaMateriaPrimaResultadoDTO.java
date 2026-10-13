package proyecto.dto;

public record CargaMateriaPrimaResultadoDTO(
        Long materiaPrimaId,
        String nombre,
        boolean creada,
        String unidadBase,
        double cantidadAgregada,
        double stockEnSede,
        double costoUnitario
) {}
