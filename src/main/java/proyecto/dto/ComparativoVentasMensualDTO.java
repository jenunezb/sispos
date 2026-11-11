package proyecto.dto;

public record ComparativoVentasMensualDTO(
        Integer anio,
        Integer mes,
        String periodoActual,
        Double totalPeriodoActual,
        String periodoAnioAnterior,
        Double totalPeriodoAnioAnterior,
        Double diferenciaAbsoluta,
        Double crecimientoPorcentual
) {
}
