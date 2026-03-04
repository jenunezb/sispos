package proyecto.servicios.interfaces;

import proyecto.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface ProduccionServicio {

    ClienteDTO crearCliente(String correoProduccion, ClienteCrearDTO dto);

    List<ClienteDTO> listarClientes(String correoProduccion);

    PrecioClienteDTO guardarPrecioCliente(String correoProduccion, Long clienteId, PrecioClienteRequestDTO dto);

    List<PrecioClienteDTO> listarPreciosCliente(String correoProduccion, Long clienteId);

    List<ProductoProduccionDTO> listarProductos(String correoProduccion);

    void registrarProduccion(String correoProduccion, ProduccionRegistroDTO dto);

    List<InventarioProduccionDTO> listarInventarioProduccion(String correoProduccion);

    InformeProduccionDiaDTO obtenerInformeDiario(String correoProduccion, LocalDate fecha);
}
