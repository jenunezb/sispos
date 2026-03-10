package proyecto.dto;

import proyecto.entidades.ModoPago;

import java.util.List;

public record VentaRecuestDTO(
        String correo,
        Long sedeId,
        Long clienteId,
        List<DetalleVentaDTO> detalles,
        ModoPago modoPago
) {}
