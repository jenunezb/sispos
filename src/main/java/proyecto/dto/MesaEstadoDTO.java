package proyecto.dto;

import java.util.List;

public record MesaEstadoDTO(
        Long id,
        Integer numero,
        String estado,
        List<MesaEstadoItemDTO> carrito,
        String nombre,
        String tipo,
        Boolean visible,
        Integer ordenVisual,
        String domicilioDireccion,
        Double domicilioCosto,
        String domicilioNombreRecibe,
        String domicilioCelularRecibe,
        Long version
) {
    public MesaEstadoDTO(Long id, Integer numero, String estado, List<MesaEstadoItemDTO> carrito, String nombre) {
        this(id, numero, estado, carrito, nombre, null, null, null, null, null, null, null, null);
    }
}
