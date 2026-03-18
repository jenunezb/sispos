package proyecto.servicios.interfaces;

import proyecto.dto.VentaRecuestDTO;
import proyecto.dto.VentaResponseDTO;
import proyecto.entidades.Venta;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaServicio {

    Venta crearVenta(VentaRecuestDTO dto);

    Venta crearVentaProduccion(String correoProduccion, VentaRecuestDTO dto);

    List<VentaResponseDTO> listarVentasPorVendedor(Long vendedorId);

    List<VentaResponseDTO> listarVentasPorVendedorEntreFechas(
            Long vendedorId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    List<VentaResponseDTO> listarVentasPorCorreoVendedor(String correoVendedor);

    List<VentaResponseDTO> listarVentasPorCorreoVendedorEntreFechas(
            String correoVendedor,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    List<VentaResponseDTO> listarVentasPorSede(Long sedeId);

    List<VentaResponseDTO> listarVentasPorSedeEntreFechas(
            Long sedeId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    VentaResponseDTO mapToResponse(Venta venta);

    void anularVenta(Long ventaId);

    void cambiarEstadoVenta(Long ventaId, Boolean valido, Long empresaNit);

    void cambiarEstadoVentaSistema(Long ventaId, Boolean valido);

    List<VentaResponseDTO> listarVentasAnuladas(Long sedeId);

    List<VentaResponseDTO> listarVentasAnuladasEntreFechas(
            Long sedeId,
            LocalDateTime desde,
            LocalDateTime hasta
    );
}
