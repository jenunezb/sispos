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
        Long mesaId,
        Boolean esDomicilio,
        String direccionDomicilio,
        Double costoDomicilio,
        String nombreRecibeDomicilio,
        String celularRecibeDomicilio
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
        this(correo, sedeId, clienteId, detalles, modoPago, montoRecibido, montoEfectivo, montoTransferencia,
                null, null, null, null, null, null);
    }

    public VentaRecuestDTO(
            String correo, Long sedeId, Long clienteId, List<DetalleVentaDTO> detalles, ModoPago modoPago,
            Double montoRecibido, Double montoEfectivo, Double montoTransferencia, Long mesaId
    ) {
        this(correo, sedeId, clienteId, detalles, modoPago, montoRecibido, montoEfectivo, montoTransferencia,
                mesaId, null, null, null, null, null);
    }
}
