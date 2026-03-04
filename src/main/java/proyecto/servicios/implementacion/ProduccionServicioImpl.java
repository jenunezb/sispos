package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.*;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.servicios.interfaces.ProduccionServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProduccionServicioImpl implements ProduccionServicio {

    private final VendedorRepository vendedorRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final PrecioClienteProductoRepository precioClienteProductoRepository;
    private final InventarioProduccionRepository inventarioProduccionRepository;
    private final MovimientoProduccionRepository movimientoProduccionRepository;

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

        Producto producto = productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getEmpresa() == null || !empresa.getNit().equals(producto.getEmpresa().getNit())) {
            throw new RuntimeException("El producto no pertenece a la empresa");
        }

        PrecioClienteProducto precio = precioClienteProductoRepository
                .findByClienteIdAndProductoCodigo(clienteId, dto.productoId())
                .orElseGet(PrecioClienteProducto::new);

        precio.setCliente(cliente);
        precio.setProducto(producto);
        precio.setPrecioVenta(dto.precio());
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

    @Override
    public List<ProductoProduccionDTO> listarProductos(String correoProduccion) {
        Empresa empresa = obtenerEmpresaProduccion(correoProduccion);

        return productoRepository.findByActivoTrueAndEmpresaNitOrderByCodigoAsc(empresa.getNit())
                .stream()
                .map(p -> new ProductoProduccionDTO(
                        p.getCodigo(),
                        p.getNombre(),
                        p.getPrecioVenta()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void registrarProduccion(String correoProduccion, ProduccionRegistroDTO dto) {
        Vendedor vendedor = obtenerVendedorProduccion(correoProduccion);
        Empresa empresa = obtenerEmpresaDesdeVendedor(vendedor);
        Sede sede = obtenerSedeDesdeVendedor(vendedor);

        Producto producto = productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getEmpresa() == null || !empresa.getNit().equals(producto.getEmpresa().getNit())) {
            throw new RuntimeException("El producto no pertenece a la empresa del perfil produccion");
        }

        InventarioProduccion inventario = inventarioProduccionRepository
                .findByProductoCodigoAndSedeId(producto.getCodigo(), sede.getId())
                .orElseGet(() -> crearInventarioProduccion(producto, sede));

        inventario.setStockActual(inventario.getStockActual() + dto.cantidad());
        inventario.setProducidoAcumulado(inventario.getProducidoAcumulado() + dto.cantidad());
        inventarioProduccionRepository.save(inventario);

        MovimientoProduccion mov = new MovimientoProduccion();
        mov.setProducto(producto);
        mov.setSede(sede);
        mov.setVendedor(vendedor);
        mov.setTipo(TipoMovimientoProduccion.PRODUCCION);
        mov.setCantidad(dto.cantidad());
        mov.setObservacion(dto.observacion());

        movimientoProduccionRepository.save(mov);
    }

    @Override
    public List<InventarioProduccionDTO> listarInventarioProduccion(String correoProduccion) {
        Vendedor vendedor = obtenerVendedorProduccion(correoProduccion);
        Sede sede = obtenerSedeDesdeVendedor(vendedor);

        return inventarioProduccionRepository
                .findBySedeIdAndProductoActivoTrueOrderByProductoCodigoAsc(sede.getId())
                .stream()
                .map(i -> new InventarioProduccionDTO(
                        i.getProducto().getCodigo(),
                        i.getProducto().getNombre(),
                        i.getStockActual(),
                        i.getProducidoAcumulado(),
                        i.getDespachadoAcumulado()
                ))
                .toList();
    }

    @Override
    public InformeProduccionDiaDTO obtenerInformeDiario(String correoProduccion, LocalDate fecha) {
        Vendedor vendedor = obtenerVendedorProduccion(correoProduccion);
        Sede sede = obtenerSedeDesdeVendedor(vendedor);

        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        List<MovimientoProduccion> movimientos = movimientoProduccionRepository
                .findBySedeIdAndFechaBetweenOrderByFechaAsc(sede.getId(), inicio, fin);

        Map<Long, ProductoDiaAcumulado> porProducto = new LinkedHashMap<>();
        Map<Long, ClienteDiaAcumulado> porCliente = new LinkedHashMap<>();

        int totalProducido = 0;
        int totalDespachado = 0;

        for (MovimientoProduccion m : movimientos) {
            Long productoId = m.getProducto().getCodigo();
            ProductoDiaAcumulado p = porProducto.computeIfAbsent(
                    productoId,
                    k -> new ProductoDiaAcumulado(productoId, m.getProducto().getNombre())
            );

            if (m.getTipo() == TipoMovimientoProduccion.PRODUCCION) {
                p.producido += m.getCantidad();
                totalProducido += m.getCantidad();
            } else {
                p.despachado += m.getCantidad();
                totalDespachado += m.getCantidad();

                if (m.getCliente() != null) {
                    Long clienteId = m.getCliente().getId();
                    ClienteDiaAcumulado c = porCliente.computeIfAbsent(
                            clienteId,
                            k -> new ClienteDiaAcumulado(clienteId, m.getCliente().getNombre())
                    );
                    c.unidades += m.getCantidad();
                }
            }
        }

        List<ResumenProductoProduccionDTO> productos = porProducto.values()
                .stream()
                .map(p -> {
                    InventarioProduccion inv = inventarioProduccionRepository
                            .findByProductoCodigoAndSedeId(p.productoId, sede.getId())
                            .orElse(null);

                    int stockFinal = inv != null ? inv.getStockActual() : 0;
                    int stockInicial = stockFinal - p.producido + p.despachado;

                    return new ResumenProductoProduccionDTO(
                            p.productoId,
                            p.productoNombre,
                            stockInicial,
                            p.producido,
                            p.despachado,
                            stockFinal
                    );
                })
                .toList();

        List<DespachoClienteProduccionDTO> clientes = porCliente.values()
                .stream()
                .map(c -> new DespachoClienteProduccionDTO(c.clienteId, c.clienteNombre, c.unidades))
                .toList();

        return new InformeProduccionDiaDTO(
                fecha,
                totalProducido,
                totalDespachado,
                productos,
                clientes
        );
    }

    private InventarioProduccion crearInventarioProduccion(Producto producto, Sede sede) {
        InventarioProduccion inv = new InventarioProduccion();
        inv.setProducto(producto);
        inv.setSede(sede);
        inv.setStockActual(0);
        inv.setProducidoAcumulado(0);
        inv.setDespachadoAcumulado(0);
        return inv;
    }

    private Empresa obtenerEmpresaProduccion(String correoProduccion) {
        Vendedor vendedor = obtenerVendedorProduccion(correoProduccion);
        return obtenerEmpresaDesdeVendedor(vendedor);
    }

    private Vendedor obtenerVendedorProduccion(String correoProduccion) {
        Vendedor vendedor = vendedorRepository.findByCorreo(correoProduccion)
                .orElseThrow(() -> new RuntimeException("Usuario de produccion no encontrado"));

        if (vendedor.getTipoPerfil() != TipoPerfilVendedor.PRODUCCION) {
            throw new RuntimeException("La cuenta no tiene perfil de produccion");
        }

        return vendedor;
    }

    private Empresa obtenerEmpresaDesdeVendedor(Vendedor vendedor) {
        if (vendedor.getEmpresa() != null) {
            return vendedor.getEmpresa();
        }

        if (vendedor.getSede() != null && vendedor.getSede().getEmpresa() != null) {
            return vendedor.getSede().getEmpresa();
        }

        throw new RuntimeException("El perfil de produccion no tiene empresa asociada");
    }

    private Sede obtenerSedeDesdeVendedor(Vendedor vendedor) {
        if (vendedor.getSede() == null) {
            throw new RuntimeException("El perfil de produccion no tiene sede asociada");
        }
        return vendedor.getSede();
    }

    private static class ProductoDiaAcumulado {
        private final Long productoId;
        private final String productoNombre;
        private int producido;
        private int despachado;

        private ProductoDiaAcumulado(Long productoId, String productoNombre) {
            this.productoId = productoId;
            this.productoNombre = productoNombre;
        }
    }

    private static class ClienteDiaAcumulado {
        private final Long clienteId;
        private final String clienteNombre;
        private int unidades;

        private ClienteDiaAcumulado(Long clienteId, String clienteNombre) {
            this.clienteId = clienteId;
            this.clienteNombre = clienteNombre;
        }
    }
}
