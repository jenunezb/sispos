package proyecto.dto;

import java.util.List;

public record VentaRecuestDTO(
        Long vendedorId,
        Long sedeId,
        List<DetalleVentaDTO> detalles
) {}
