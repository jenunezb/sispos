package proyecto.dto;

public record VentasPorHoraDTO(
        Integer hora,
        String etiquetaHora,
        Double totalVentas,
        Long cantidadVentas
) {
}
