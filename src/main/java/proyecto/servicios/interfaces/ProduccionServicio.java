package proyecto.servicios.interfaces;

import proyecto.dto.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ProduccionServicio {

    ClienteDTO crearCliente(String correoProduccion, ClienteCrearDTO dto);

    List<ClienteDTO> listarClientes(String correoProduccion);

    ClienteDTO actualizarCliente(String correoProduccion, Long clienteId, ClienteActualizarDTO dto);

    void eliminarCliente(String correoProduccion, Long clienteId);

    PrecioClienteDTO guardarPrecioCliente(String correoProduccion, Long clienteId, PrecioClienteRequestDTO dto);

    List<PrecioClienteDTO> listarPreciosCliente(String correoProduccion, Long clienteId);

    List<ProductoProduccionDTO> listarProductos(String correoProduccion);

    ProductoProduccionDTO crearProducto(String correoProduccion, ProductoProduccionRequestDTO dto);

    ProductoProduccionDTO actualizarProducto(String correoProduccion, Long productoId, ProductoProduccionRequestDTO dto);

    void eliminarProducto(String correoProduccion, Long productoId);

    String registrarProduccion(String correoProduccion, ProduccionRegistroDTO dto);

    String registrarProduccionMultiple(String correoProduccion, ProduccionRegistroMultipleDTO dto);

    List<InventarioProduccionDTO> listarInventario(String correoProduccion);

    ProduccionAjusteManualResponseDTO listarInventarioAjustable(String correoProduccion);

    ProduccionAjusteManualResultadoResponseDTO ajustarInventarioManual(
            String correoProduccion,
            ProduccionAjusteManualRequestDTO dto
    );

    AdminPinEstadoResponseDTO obtenerEstadoAdminPin(String correoProduccion);

    AdminPinValidacionResponseDTO validarAdminPin(String correoProduccion, AdminPinValidacionRequestDTO dto);

    AdminPinMensajeResponseDTO actualizarAdminPin(String correoProduccion, AdminPinActualizarRequestDTO dto);

    List<VentaResponseDTO> listarVentas(String correoProduccion);

    List<VentaResponseDTO> listarVentasRango(String correoProduccion, LocalDateTime desde, LocalDateTime hasta);

    InformeProduccionDiaDTO obtenerInformeDiario(String correoProduccion, LocalDate fecha);
}
