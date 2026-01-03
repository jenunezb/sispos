package proyecto.dto;

import proyecto.entidades.ModoPago;

import java.util.List;

public record VentaRecuestDTO(
        Long vendedorId,
        Long sedeId,
        List<DetalleVentaDTO> detalles,
        ModoPago modoPago
) {}
