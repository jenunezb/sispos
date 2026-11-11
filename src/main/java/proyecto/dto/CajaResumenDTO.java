package proyecto.dto;

public record CajaResumenDTO(
        Double baseInicial,
        Double ventasEfectivo,
        Double gastosEfectivo,
        Double efectivoEsperado,
        Double efectivoContado,
        Double diferencia
) {
}
