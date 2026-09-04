package proyecto.dto;

import proyecto.entidades.ModoPago;

import java.util.List;

public record VentaRecuestDTO(
        String correo,
        Long sedeId,
        Long clienteId,
        List<DetalleVentaDTO> detalles,
        ModoPago modoPago,
        Double montoRecibido,
        Double montoEfectivo,
        Double montoTransferencia,
        Long mesaId
) {
    public VentaRecuestDTO(
            String correo,
            Long sedeId,
            Long clienteId,
            List<DetalleVentaDTO> detalles,
            ModoPago modoPago,
            Double montoRecibido,
            Double montoEfectivo,
            Double montoTransferencia
    ) {
        this(correo, sedeId, clienteId, detalles, modoPago, montoRecibido, montoEfectivo, montoTransferencia, null);
    }
}
