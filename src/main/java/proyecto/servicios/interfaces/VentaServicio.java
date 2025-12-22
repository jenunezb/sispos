package proyecto.servicios.interfaces;

import proyecto.dto.VentaRecuestDTO;
import proyecto.dto.VentaResponseDTO;
import proyecto.entidades.Vendedor;
import proyecto.entidades.Venta;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaServicio {

    Venta crearVenta(VentaRecuestDTO dto);

    List<VentaResponseDTO> listarVentasPorVendedor(Long vendedorId);

    List<VentaResponseDTO> listarVentasPorVendedorEntreFechas(
            Long vendedorId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    List<VentaResponseDTO> listarVentasPorSede(Long sedeId);

    List<VentaResponseDTO> listarVentasPorSedeEntreFechas(
            Long sedeId,
            LocalDateTime desde,
            LocalDateTime hasta
    );

}