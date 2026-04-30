package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.*;
import proyecto.entidades.*;
import proyecto.repositorios.ClienteRepository;
import proyecto.repositorios.InventarioProduccionRepository;
import proyecto.repositorios.MovimientoProduccionRepository;
import proyecto.repositorios.PrecioClienteProductoRepository;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.servicios.interfaces.ProduccionServicio;
import proyecto.servicios.interfaces.VentaServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProduccionServicioImpl implements ProduccionServicio {

    private final VendedorRepository vendedorRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final PrecioClienteProductoRepository precioClienteProductoRepository;
    private final InventarioProduccionRepository inventarioProduccionRepository;
    private final MovimientoProduccionRepository movimientoProduccionRepository;
    private final VentaServicio ventaServicio;

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
    public String registrarProduccion(String correoProduccion, ProduccionRegistroDTO dto) {
        Vendedor vendedor = obtenerVendedorProduccion(correoProduccion);
        Sede sede = obtenerSedeProduccion(vendedor);
        Empresa empresa = obtenerEmpresaDesdeVendedor(vendedor);

        Producto producto = productoRepository.findById(dto.productoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getEmpresa() == null || !empresa.getNit().equals(producto.getEmpresa().getNit())) {
            throw new RuntimeException("El producto no pertenece a la empresa del perfil de produccion");
        }

        InventarioProduccion inventario = inventarioProduccionRepository
                .findByProductoCodigoAndSedeId(producto.getCodigo(), sede.getId())
                .orElseGet(() -> {
                    InventarioProduccion nuevo = new InventarioProduccion();
                    nuevo.setProducto(producto);
                    nuevo.setSede(sede);
                    nuevo.setStockActual(0);
                    nuevo.setProducidoAcumulado(0);
                    nuevo.setDespachadoAcumulado(0);
                    return nuevo;
                });

        inventario.setStockActual(inventario.getStockActual() + dto.cantidad());
        inventario.setProducidoAcumulado(inventario.getProducidoAcumulado() + dto.cantidad());
        inventarioProduccionRepository.save(inventario);

        MovimientoProduccion movimiento = new MovimientoProduccion();
        movimiento.setProducto(producto);
        movimiento.setSede(sede);
        movimiento.setCliente(null);
        movimiento.setVendedor(vendedor);
        movimiento.setTipo(TipoMovimientoProduccion.PRODUCCION);
        movimiento.setCantidad(dto.cantidad());
        movimiento.setObservacion(dto.observacion());
        movimientoProduccionRepository.save(movimiento);

        return "Produccion registrada correctamente";
    }

    @Override
    public List<InventarioProduccionDTO> listarInventario(String correoProduccion) {
        Sede sede = obtenerSedeProduccion(obtenerVendedorProduccion(correoProduccion));

        return inventarioProduccionRepository.findBySedeIdAndProductoActivoTrueOrderByProductoCodigoAsc(sede.getId())
                .stream()
                .map(item -> new InventarioProduccionDTO(
                        item.getProducto().getCodigo(),
                        item.getProducto().getNombre(),
                        item.getStockActual(),
                        item.getProducidoAcumulado(),
                        item.getDespachadoAcumulado()
                ))
                .toList();
    }

    @Override
    public List<VentaResponseDTO> listarVentas(String correoProduccion) {
        obtenerVendedorProduccion(correoProduccion);
        return ventaServicio.listarVentasPorCorreoVendedor(correoProduccion);
    }

    @Override
    public List<VentaResponseDTO> listarVentasRango(String correoProduccion, LocalDateTime desde, LocalDateTime hasta) {
        obtenerVendedorProduccion(correoProduccion);
        return ventaServicio.listarVentasPorCorreoVendedorEntreFechas(correoProduccion, desde, hasta);
    }

    @Override
    public InformeProduccionDiaDTO obtenerInformeDiario(String correoProduccion, LocalDate fecha) {
        Sede sede = obtenerSedeProduccion(obtenerVendedorProduccion(correoProduccion));
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);

        List<MovimientoProduccion> movimientos = movimientoProduccionRepository
                .findBySedeIdAndFechaBetweenOrderByFechaAsc(sede.getId(), inicio, fin);

        Map<Long, List<MovimientoProduccion>> movimientosPorProducto = movimientos.stream()
                .filter(mov -> mov.getProducto() != null)
                .collect(Collectors.groupingBy(mov -> mov.getProducto().getCodigo()));

        List<ResumenProductoProduccionDTO> productos = inventarioProduccionRepository
                .findBySedeIdAndProductoActivoTrueOrderByProductoCodigoAsc(sede.getId())
                .stream()
                .map(inventario -> {
                    List<MovimientoProduccion> movimientosProducto = movimientosPorProducto
                            .getOrDefault(inventario.getProducto().getCodigo(), List.of());

                    int producido = movimientosProducto.stream()
                            .filter(mov -> mov.getTipo() == TipoMovimientoProduccion.PRODUCCION)
                            .mapToInt(MovimientoProduccion::getCantidad)
                            .sum();

                    int despachado = movimientosProducto.stream()
                            .filter(mov -> mov.getTipo() == TipoMovimientoProduccion.DESPACHO)
                            .mapToInt(MovimientoProduccion::getCantidad)
                            .sum();

                    int stockFinal = inventario.getStockActual();
                    int stockInicial = stockFinal - producido + despachado;

                    return new ResumenProductoProduccionDTO(
                            inventario.getProducto().getCodigo(),
                            inventario.getProducto().getNombre(),
                            stockInicial,
                            producido,
                            despachado,
                            stockFinal
                    );
                })
                .toList();

        List<DespachoClienteProduccionDTO> despachosPorCliente = movimientos.stream()
                .filter(mov -> mov.getTipo() == TipoMovimientoProduccion.DESPACHO && mov.getCliente() != null)
                .collect(Collectors.groupingBy(mov -> mov.getCliente().getId()))
                .values()
                .stream()
                .map(items -> {
                    MovimientoProduccion primero = items.get(0);
                    int totalUnidades = items.stream().mapToInt(MovimientoProduccion::getCantidad).sum();
                    return new DespachoClienteProduccionDTO(
                            primero.getCliente().getId(),
                            primero.getCliente().getNombre(),
                            totalUnidades
                    );
                })
                .sorted(Comparator.comparing(DespachoClienteProduccionDTO::clienteNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int totalProducido = productos.stream().mapToInt(ResumenProductoProduccionDTO::producido).sum();
        int totalDespachado = productos.stream().mapToInt(ResumenProductoProduccionDTO::despachado).sum();

        return new InformeProduccionDiaDTO(
                fecha,
                totalProducido,
                totalDespachado,
                productos,
                despachosPorCliente
        );
    }

    private Empresa obtenerEmpresaProduccion(String correoProduccion) {
        return obtenerEmpresaDesdeVendedor(obtenerVendedorProduccion(correoProduccion));
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

    private Sede obtenerSedeProduccion(Vendedor vendedor) {
        if (vendedor.getSede() == null) {
            throw new RuntimeException("El perfil de produccion no tiene sede asociada");
        }
        return vendedor.getSede();
    }
}
