package proyecto.dto;

import java.util.List;

public record MesaEstadoDTO(
        Long id,
        Integer numero,
        String estado,
        List<MesaEstadoItemDTO> carrito,
        String nombre
) {}
