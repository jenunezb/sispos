package proyecto.dto;

public record AjusteManualResultadoItemDTO(
        Long id,
        TipoInventarioAjustable tipo,
        Double stockAnterior,
        Double stockNuevo
) {
}
