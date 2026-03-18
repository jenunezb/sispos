package proyecto.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SuperAdminVentaDTO(
        Long ventaId,
        LocalDateTime fecha,
        Double total,
        Boolean anulado,
        Boolean valido,
        String modoPago,
        Long empresaNit,
        String empresaNombre,
        Long sedeId,
        String sedeUbicacion,
        Integer vendedorId,
        String vendedorNombre,
        String vendedorCorreo,
        Integer administradorId,
        String administradorNombre,
        String administradorCorreo,
        Long clienteId,
        String clienteNombre,
        String clienteTelefono,
        String clienteDocumento,
        List<SuperAdminDetalleVentaDTO> detalles
) {
}
