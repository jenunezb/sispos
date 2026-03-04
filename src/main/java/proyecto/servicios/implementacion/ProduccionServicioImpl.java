package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.*;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.servicios.interfaces.ProduccionServicio;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProduccionServicioImpl implements ProduccionServicio {

    private final VendedorRepository vendedorRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final PrecioClienteProductoRepository precioClienteProductoRepository;

    @Override
    @Transactional
    public ClienteDTO crearCliente(String correoProduccion, ClienteCrearDTO dto) {
        Empresa empresa = obtenerEmpresaProduccion(correoProduccion);

        Cliente cliente = new Cliente();
        cliente.setNombre(dto.nombre());
        cliente.setTelefono(dto.telefono());
        cliente.setDocumento(dto.documento());
        cliente.setEmpresa(empresa);
        cliente.setActivo(true);

        Cliente guardado = clienteRepository.save(cliente);

        return new ClienteDTO(
                guardado.getId(),
                guardado.getNombre(),
                guardado.getTelefono(),
                guardado.getDocumento(),
                guardado.getActivo()
        );
    }

    @Override
    public List<ClienteDTO> listarClientes(String correoProduccion) {
        Empresa empresa = obtenerEmpresaProduccion(correoProduccion);

        return clienteRepository.findByEmpresaNitAndActivoTrueOrderByNombreAsc(empresa.getNit())
                .stream()
                .map(c -> new ClienteDTO(c.getId(), c.getNombre(), c.getTelefono(), c.getDocumento(), c.getActivo()))
                .toList();
    }

    @Override
    @Transactional
    public PrecioClienteDTO guardarPrecioCliente(String correoProduccion, Long clienteId, PrecioClienteRequestDTO dto) {
        Empresa empresa = obtenerEmpresaProduccion(correoProduccion);

        Cliente cliente = clienteRepository.findByIdAndEmpresaNit(clienteId, empresa.getNit())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado para la empresa"));

        Producto producto = productoRepository.findById(dto.productoCodigo())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getEmpresa() == null || !empresa.getNit().equals(producto.getEmpresa().getNit())) {
            throw new RuntimeException("El producto no pertenece a la empresa");
        }

        PrecioClienteProducto precio = precioClienteProductoRepository
                .findByClienteIdAndProductoCodigo(clienteId, dto.productoCodigo())
                .orElseGet(PrecioClienteProducto::new);

        precio.setCliente(cliente);
        precio.setProducto(producto);
        precio.setPrecioVenta(dto.precioVenta());
        precio.setActivo(true);

        PrecioClienteProducto guardado = precioClienteProductoRepository.save(precio);

        return new PrecioClienteDTO(
                guardado.getId(),
                cliente.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                guardado.getPrecioVenta(),
                guardado.getActivo()
        );
    }

    @Override
    public List<PrecioClienteDTO> listarPreciosCliente(String correoProduccion, Long clienteId) {
        Empresa empresa = obtenerEmpresaProduccion(correoProduccion);

        return precioClienteProductoRepository
                .findByClienteIdAndClienteEmpresaNitAndActivoTrueOrderByProductoNombreAsc(clienteId, empresa.getNit())
                .stream()
                .map(p -> new PrecioClienteDTO(
                        p.getId(),
                        p.getCliente().getId(),
                        p.getProducto().getCodigo(),
                        p.getProducto().getNombre(),
                        p.getPrecioVenta(),
                        p.getActivo()
                ))
                .toList();
    }

    private Empresa obtenerEmpresaProduccion(String correoProduccion) {
        Vendedor vendedor = vendedorRepository.findByCorreo(correoProduccion)
                .orElseThrow(() -> new RuntimeException("Usuario de producción no encontrado"));

        if (vendedor.getTipoPerfil() != TipoPerfilVendedor.PRODUCCION) {
            throw new RuntimeException("La cuenta no tiene perfil de producción");
        }

        if (vendedor.getEmpresa() != null) {
            return vendedor.getEmpresa();
        }

        if (vendedor.getSede() != null && vendedor.getSede().getEmpresa() != null) {
            return vendedor.getSede().getEmpresa();
        }

        throw new RuntimeException("El perfil de producción no tiene empresa asociada");
    }
}
