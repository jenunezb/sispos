package proyecto.servicios.interfaces;

import proyecto.dto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ProduccionServicio {

    ClienteDTO crearCliente(String correoProduccion, ClienteCrearDTO dto);

    List<ClienteDTO> listarClientes(String correoProduccion);

    PrecioClienteDTO guardarPrecioCliente(String correoProduccion, Long clienteId, PrecioClienteRequestDTO dto);

    List<PrecioClienteDTO> listarPreciosCliente(String correoProduccion, Long clienteId);

    List<ProductoProduccionDTO> listarProductos(String correoProduccion);

    String registrarProduccion(String correoProduccion, ProduccionRegistroDTO dto);

    List<InventarioProduccionDTO> listarInventario(String correoProduccion);

    List<VentaResponseDTO> listarVentas(String correoProduccion);

    List<VentaResponseDTO> listarVentasRango(String correoProduccion, LocalDateTime desde, LocalDateTime hasta);

    InformeProduccionDiaDTO obtenerInformeDiario(String correoProduccion, LocalDate fecha);
}
