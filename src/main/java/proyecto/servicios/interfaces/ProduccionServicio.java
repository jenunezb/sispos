package proyecto.servicios.interfaces;

import proyecto.dto.*;

import java.util.List;

public interface ProduccionServicio {

    ClienteDTO crearCliente(String correoProduccion, ClienteCrearDTO dto);

    List<ClienteDTO> listarClientes(String correoProduccion);

    PrecioClienteDTO guardarPrecioCliente(String correoProduccion, Long clienteId, PrecioClienteRequestDTO dto);

    List<PrecioClienteDTO> listarPreciosCliente(String correoProduccion, Long clienteId);
}
