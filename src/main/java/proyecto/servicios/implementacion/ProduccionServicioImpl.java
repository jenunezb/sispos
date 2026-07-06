package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.*;
import proyecto.entidades.*;
import proyecto.repositorios.ClienteRepository;
import proyecto.repositorios.InventarioProduccionRepository;
import proyecto.repositorios.MovimientoProduccionRepository;
import proyecto.repositorios.PrecioClienteProductoRepository;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.servicios.interfaces.ProduccionServicio;
import proyecto.servicios.interfaces.VentaServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProduccionServicioImpl implements ProduccionServicio {

    private static final int ADMIN_PIN_MAX_INTENTOS = 5;
    private static final long ADMIN_PIN_BLOQUEO_MINUTOS = 15;

    private final VendedorRepository vendedorRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final PrecioClienteProductoRepository precioClienteProductoRepository;
    private final InventarioProduccionRepository inventarioProduccionRepository;
    private final MovimientoProduccionRepository movimientoProduccionRepository;
    private final SedeRepository sedeRepository;
    private final VentaServicio ventaServicio;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
    public ClienteDTO actualizarCliente(String correoProduccion, Long clienteId, ClienteActualizarDTO dto) {
        Empresa empresa = obtenerEmpresaProduccion(correoProduccion);

        Cliente cliente = clienteRepository.findByIdAndEmpresaNit(clienteId, empresa.getNit())
                .filter(Cliente::getActivo)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado para la empresa"));

        cliente.setNombre(dto.nombre());
        cliente.setTelefono(dto.telefono());
        cliente.setDocumento(dto.documento());

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
    @Transactional
    public void eliminarCliente(String correoProduccion, Long clienteId) {
        Empresa empresa = obtenerEmpresaProduccion(correoProduccion);

        Cliente cliente = clienteRepository.findByIdAndEmpresaNit(clienteId, empresa.getNit())
                .filter(Cliente::getActivo)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado para la empresa"));

        cliente.setActivo(false);
        clienteRepository.save(cliente);

        precioClienteProductoRepository.findByClienteId(clienteId)
                .forEach(precio -> {
                    precio.setActivo(false);
                    precioClienteProductoRepository.save(precio);
                });
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
        Vendedor vendedor = obtenerVendedorProduccion(correoProduccion);
        Sede sede = obtenerSedeProduccion(vendedor);

        return inventarioProduccionRepository.findBySedeIdAndProductoActivoTrueOrderByProductoCodigoAsc(sede.getId())
                .stream()
                .map(inventario -> new ProductoProduccionDTO(
                        inventario.getProducto().getCodigo(),
                        inventario.getProducto().getNombre(),
                        inventario.getProducto().getPrecioVenta()
                ))
                .toList();
    }

    @Override
    @Transactional
    public ProductoProduccionDTO crearProducto(String correoProduccion, ProductoProduccionRequestDTO dto) {
        Vendedor vendedor = obtenerVendedorProduccion(correoProduccion);
        Empresa empresa = obtenerEmpresaDesdeVendedor(vendedor);
        Sede sede = obtenerSedeProduccion(vendedor);

        Double precioBase = resolverPrecioBase(dto);

        Producto producto = new Producto();
        producto.setNombre(dto.nombre());
        producto.setDescripcion(dto.descripcion());
        producto.setPrecioProduccion(dto.precioProduccion() != null ? dto.precioProduccion() : precioBase);
        producto.setPrecioVenta(precioBase);
        producto.setCategoria(null);
        producto.setEstado(true);
        producto.setActivo(true);
        producto.setEmpresa(empresa);

        Producto guardado = productoRepository.save(producto);

        InventarioProduccion inventario = new InventarioProduccion();
        inventario.setProducto(guardado);
        inventario.setSede(sede);
        inventario.setStockActual(0);
        inventario.setProducidoAcumulado(0);
        inventario.setDespachadoAcumulado(0);
        inventarioProduccionRepository.save(inventario);

        return new ProductoProduccionDTO(
                guardado.getCodigo(),
                guardado.getNombre(),
                guardado.getPrecioVenta()
        );
    }

    @Override
    @Transactional
    public ProductoProduccionDTO actualizarProducto(String correoProduccion, Long productoId, ProductoProduccionRequestDTO dto) {
        Empresa empresa = obtenerEmpresaProduccion(correoProduccion);
        Double precioBase = resolverPrecioBase(dto);

        Producto producto = productoRepository.findByCodigoAndEmpresaNit(productoId, empresa.getNit())
                .filter(Producto::getActivo)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado para la empresa"));

        producto.setNombre(dto.nombre());
        producto.setDescripcion(dto.descripcion());
        producto.setPrecioProduccion(dto.precioProduccion() != null ? dto.precioProduccion() : precioBase);
        producto.setPrecioVenta(precioBase);

        Producto guardado = productoRepository.save(producto);

        return new ProductoProduccionDTO(
                guardado.getCodigo(),
                guardado.getNombre(),
                guardado.getPrecioVenta()
        );
    }

    @Override
    @Transactional
    public void eliminarProducto(String correoProduccion, Long productoId) {
        Empresa empresa = obtenerEmpresaProduccion(correoProduccion);

        Producto producto = productoRepository.findByCodigoAndEmpresaNit(productoId, empresa.getNit())
                .filter(Producto::getActivo)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado para la empresa"));

        producto.setActivo(false);
        productoRepository.save(producto);
    }

    @Override
    @Transactional
    public String registrarProduccion(String correoProduccion, ProduccionRegistroDTO dto) {
        Vendedor vendedor = obtenerVendedorProduccion(correoProduccion);
        Sede sede = obtenerSedeProduccion(vendedor);
        Empresa empresa = obtenerEmpresaDesdeVendedor(vendedor);

        registrarProduccionItem(vendedor, sede, empresa, dto.productoId(), dto.cantidad(), dto.observacion());

        return "Produccion registrada correctamente";
    }

    @Override
    @Transactional
    public String registrarProduccionMultiple(String correoProduccion, ProduccionRegistroMultipleDTO dto) {
        Vendedor vendedor = obtenerVendedorProduccion(correoProduccion);
        Sede sede = obtenerSedeProduccion(vendedor);
        Empresa empresa = obtenerEmpresaDesdeVendedor(vendedor);

        for (ProduccionRegistroItemDTO item : dto.items()) {
            registrarProduccionItem(vendedor, sede, empresa, item.productoId(), item.cantidad(), dto.observacion());
        }

        return "Produccion registrada correctamente";
    }

    private void registrarProduccionItem(
            Vendedor vendedor,
            Sede sede,
            Empresa empresa,
            Long productoId,
            Integer cantidad,
            String observacion
    ) {

        Producto producto = productoRepository.findById(productoId)
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

        inventario.setStockActual(inventario.getStockActual() + cantidad);
        inventario.setProducidoAcumulado(inventario.getProducidoAcumulado() + cantidad);
        inventarioProduccionRepository.save(inventario);

        MovimientoProduccion movimiento = new MovimientoProduccion();
        movimiento.setProducto(producto);
        movimiento.setSede(sede);
        movimiento.setCliente(null);
        movimiento.setVendedor(vendedor);
        movimiento.setTipo(TipoMovimientoProduccion.PRODUCCION);
        movimiento.setCantidad(cantidad);
        movimiento.setObservacion(observacion);
        movimientoProduccionRepository.save(movimiento);
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
    public ProduccionAjusteManualResponseDTO listarInventarioAjustable(String correoProduccion) {
        Sede sede = obtenerSedeProduccion(obtenerVendedorProduccion(correoProduccion));

        List<ProduccionAjusteManualItemDTO> items = inventarioProduccionRepository
                .findBySedeIdAndProductoActivoTrueOrderByProductoCodigoAsc(sede.getId())
                .stream()
                .map(inventario -> new ProduccionAjusteManualItemDTO(
                        inventario.getProducto().getCodigo(),
                        inventario.getProducto().getNombre(),
                        inventario.getStockActual()
                ))
                .toList();

        return new ProduccionAjusteManualResponseDTO(items);
    }

    @Override
    @Transactional
    public ProduccionAjusteManualResultadoResponseDTO ajustarInventarioManual(
            String correoProduccion,
            ProduccionAjusteManualRequestDTO dto
    ) {
        if (dto.items() == null || dto.items().isEmpty()) {
            throw new IllegalArgumentException("Debe enviar al menos un item para ajustar");
        }

        Sede sede = obtenerSedeProduccion(obtenerVendedorProduccion(correoProduccion));
        Map<Long, Boolean> itemsProcesados = new HashMap<>();
        dto.items().forEach(item -> {
            if (itemsProcesados.putIfAbsent(item.productoId(), true) != null) {
                throw new IllegalArgumentException("Hay productos repetidos en la solicitud: " + item.productoId());
            }
        });

        List<ProduccionAjusteManualResultadoItemDTO> resultado = dto.items().stream()
                .map(item -> ajustarInventarioItem(sede, item))
                .filter(java.util.Objects::nonNull)
                .toList();

        return new ProduccionAjusteManualResultadoResponseDTO(resultado.size(), resultado);
    }

    @Override
    public AdminPinEstadoResponseDTO obtenerEstadoAdminPin(String correoProduccion) {
        Sede sede = obtenerSedeProduccion(obtenerVendedorProduccion(correoProduccion));
        return new AdminPinEstadoResponseDTO(pinConfigurado(sede));
    }

    @Override
    @Transactional
    public AdminPinValidacionResponseDTO validarAdminPin(String correoProduccion, AdminPinValidacionRequestDTO dto) {
        validarFormatoPin(dto.pin(), "El PIN debe tener exactamente 4 digitos numericos");

        Sede sede = obtenerSedeProduccionConBloqueo(correoProduccion);
        if (!pinConfigurado(sede)) {
            return new AdminPinValidacionResponseDTO(false, "No hay PIN configurado para esta sede");
        }

        LocalDateTime ahora = LocalDateTime.now();
        if (sede.getAdminPinBloqueadoHasta() != null && sede.getAdminPinBloqueadoHasta().isAfter(ahora)) {
            return new AdminPinValidacionResponseDTO(false, "PIN bloqueado temporalmente. Intenta mas tarde");
        }

        if (!passwordEncoder.matches(dto.pin(), sede.getAdminPinHash())) {
            int intentos = (sede.getAdminPinIntentosFallidos() == null ? 0 : sede.getAdminPinIntentosFallidos()) + 1;
            sede.setAdminPinIntentosFallidos(intentos);

            if (intentos >= ADMIN_PIN_MAX_INTENTOS) {
                sede.setAdminPinBloqueadoHasta(ahora.plusMinutes(ADMIN_PIN_BLOQUEO_MINUTOS));
                sede.setAdminPinIntentosFallidos(0);
                sedeRepository.save(sede);
                return new AdminPinValidacionResponseDTO(false, "PIN bloqueado temporalmente. Intenta mas tarde");
            }

            sede.setAdminPinBloqueadoHasta(null);
            sedeRepository.save(sede);
            return new AdminPinValidacionResponseDTO(false, "PIN invalido");
        }

        sede.setAdminPinIntentosFallidos(0);
        sede.setAdminPinBloqueadoHasta(null);
        sedeRepository.save(sede);
        return new AdminPinValidacionResponseDTO(true, "PIN valido");
    }

    @Override
    @Transactional
    public AdminPinMensajeResponseDTO actualizarAdminPin(String correoProduccion, AdminPinActualizarRequestDTO dto) {
        validarFormatoPin(dto.pinNuevo(), "El PIN nuevo debe tener exactamente 4 digitos numericos");

        Sede sede = obtenerSedeProduccionConBloqueo(correoProduccion);
        if (pinConfigurado(sede)) {
            validarFormatoPin(dto.pinActual(), "El PIN actual debe tener exactamente 4 digitos numericos");
            if (!passwordEncoder.matches(dto.pinActual(), sede.getAdminPinHash())) {
                throw new RuntimeException("El PIN actual es incorrecto");
            }
        }

        sede.setAdminPinHash(passwordEncoder.encode(dto.pinNuevo()));
        sede.setAdminPinIntentosFallidos(0);
        sede.setAdminPinBloqueadoHasta(null);
        sedeRepository.save(sede);

        return new AdminPinMensajeResponseDTO("PIN actualizado correctamente");
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

    private Sede obtenerSedeProduccionConBloqueo(String correoProduccion) {
        Sede sede = obtenerSedeProduccion(obtenerVendedorProduccion(correoProduccion));
        return sedeRepository.findByIdForUpdate(sede.getId())
                .orElseThrow(() -> new RuntimeException("Sede de produccion no encontrada"));
    }

    private ProduccionAjusteManualResultadoItemDTO ajustarInventarioItem(
            Sede sede,
            ProduccionAjusteManualRequestItemDTO item
    ) {
        InventarioProduccion inventario = inventarioProduccionRepository
                .findByProductoCodigoAndSedeIdAndProductoActivoTrue(item.productoId(), sede.getId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado en inventario de produccion"));

        Integer stockAnterior = inventario.getStockActual();
        if (stockAnterior.equals(item.stockNuevo())) {
            return null;
        }

        inventario.setStockActual(item.stockNuevo());
        inventarioProduccionRepository.save(inventario);

        return new ProduccionAjusteManualResultadoItemDTO(
                item.productoId(),
                stockAnterior,
                inventario.getStockActual()
        );
    }

    private Double resolverPrecioBase(ProductoProduccionRequestDTO dto) {
        Double precioBase = dto.precioBase() != null ? dto.precioBase() : dto.precioVenta();

        if (precioBase == null) {
            throw new RuntimeException("El precio base es obligatorio");
        }

        return precioBase;
    }

    private boolean pinConfigurado(Sede sede) {
        return sede.getAdminPinHash() != null && !sede.getAdminPinHash().isBlank();
    }

    private void validarFormatoPin(String pin, String mensajeError) {
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new RuntimeException(mensajeError);
        }
    }
}
