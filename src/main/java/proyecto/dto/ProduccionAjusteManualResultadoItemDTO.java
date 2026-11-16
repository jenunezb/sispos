package proyecto.dto;

public record ProduccionAjusteManualResultadoItemDTO(
        Long productoId,
        Integer stockAnterior,
        Integer stockNuevo
) {
}
