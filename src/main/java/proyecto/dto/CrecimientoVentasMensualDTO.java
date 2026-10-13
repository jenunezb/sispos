package proyecto.dto;

public record CrecimientoVentasMensualDTO(
        Integer anio,
        Integer mes,
        String periodo,
        Double totalVentas,
        Double totalMesAnterior,
        Double crecimientoPorcentual
) {
}
