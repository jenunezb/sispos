package proyecto.dto;

public record VentasPorMesDTO(
        Integer anio,
        Integer mes,
        String periodo,
        Double totalVentas,
        Long cantidadVentas
) {
}
