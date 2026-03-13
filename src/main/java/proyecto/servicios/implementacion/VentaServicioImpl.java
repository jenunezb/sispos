package proyecto.servicios.implementacion;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.DetalleVentaDTO;
import proyecto.dto.DetalleVentaResponseDTO;
import proyecto.dto.VentaRecuestDTO;
import proyecto.dto.VentaResponseDTO;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.servicios.interfaces.VentaServicio;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VentaServicioImpl implements VentaServicio {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final VendedorRepository vendedorRepository;
    private final SedeRepository sedeRepository;
    private final MateriaPrimaSedeRepository materiaPrimaSedeRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final InventarioRepository inventarioRepository;
    private final AdministradorRepository administradorRepository;
    private final ClienteRepository clienteRepository;
    private final PrecioClienteProductoRepository precioClienteProductoRepository;
    private final InventarioProduccionRepository inventarioProduccionRepository;
    private final MovimientoProduccionRepository movimientoProduccionRepository;
    private final NotificacionStockMinimoService notificacionStockMinimoService;

    @Override
    @Transactional(timeout = 20)
    public Venta crearVenta(VentaRecuestDTO dto) {
        return crearVentaInterna(dto, false);
    }

    @Override
    @Transactional(timeout = 20)
    public Venta crearVentaProduccion(String correoProduccion, VentaRecuestDTO dto) {
        VentaRecuestDTO dtoConCorreo = new VentaRecuestDTO(
                correoProduccion,
                dto.sedeId(),
                dto.clienteId(),
                dto.detalles(),
                dto.modoPago()
        );
        return crearVentaInterna(dtoConCorreo, true);
    }

    private Venta crearVentaInterna(VentaRecuestDTO dto, boolean exigirPerfilProduccion) {

        if (dto.detalles() == null || dto.detalles().isEmpty()) {
            throw new RuntimeException("La venta debe contener al menos un detalle");
        }

        String correo = dto.correo() == null ? "" : dto.correo().trim();
        Optional<Vendedor> vendedorOpt = vendedorRepository.findByCorreoIgnoreCase(correo);
        Optional<Administrador> adminOpt = administradorRepository.findByCorreoIgnoreCase(correo);

        Vendedor vendedor = null;
        Administrador administrador = null;

        if (exigirPerfilProduccion) {
            vendedor = vendedorOpt
                    .orElseThrow(() -> new RuntimeException("Usuario de produccion no autorizado"));

            if (vendedor.getTipoPerfil() != TipoPerfilVendedor.PRODUCCION) {
                throw new RuntimeException("Solo el perfil de produccion puede usar este recurso");
            }
        } else {
            // En flujo general priorizamos administrador para evitar clasificar su venta como produccion
            // cuando hay correos duplicados entre tablas de usuarios heredadas.
            if (adminOpt.isPresent()) {
                administrador = adminOpt.get();
            } else if (vendedorOpt.isPresent()) {
                vendedor = vendedorOpt.get();
            } else {
                throw new RuntimeException("Usuario no autorizado");
            }
        }

        Sede sede;
        if (esVendedorProduccion(vendedor)) {
            Sede sedeProduccion = obtenerSedeDesdeVendedor(vendedor);
            sede = sedeProduccion;
        } else {
            if (dto.sedeId() == null) {
                throw new RuntimeException("Sede no encontrada");
            }
            sede = sedeRepository.findById(dto.sedeId())
                    .orElseThrow(() -> new RuntimeException("Sede no encontrada"));
        }

        Cliente cliente = null;
        if (dto.clienteId() != null) {
            cliente = clienteRepository.findById(dto.clienteId())
                    .filter(Cliente::getActivo)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado o inactivo"));
        }

        if (esVendedorProduccion(vendedor)) {
            if (cliente == null) {
                throw new RuntimeException("Para perfil produccion el cliente es obligatorio");
            }

            Empresa empresaProduccion = obtenerEmpresaVendedor(vendedor);
            if (cliente.getEmpresa() == null || !empresaProduccion.getNit().equals(cliente.getEmpresa().getNit())) {
                throw new RuntimeException("El cliente no pertenece a la empresa del perfil produccion");
            }
        }

        Venta venta = new Venta();
        venta.setFecha(ZonedDateTime.now(ZoneId.of("America/Bogota")).toLocalDateTime());
        venta.setVendedor(vendedor);
        venta.setAdministrador(administrador);
        venta.setSede(sede);
        venta.setCliente(cliente);
        venta.setModoPago(dto.modoPago() != null ? dto.modoPago() : ModoPago.EFECTIVO);

        double total = 0;
        List<DetalleVenta> detalles = new ArrayList<>();

        boolean ventaProduccion = esVendedorProduccion(vendedor);

        for (DetalleVentaDTO d : dto.detalles()) {

            if (d.cantidad() == null || d.cantidad() <= 0) {
                throw new RuntimeException("La cantidad del detalle debe ser mayor a cero");
            }

            DetalleVenta detalle = new DetalleVenta();
            detalle.setCantidad(d.cantidad());
            detalle.setVenta(venta);

            if (d.productoId() != null) {

                Producto producto = productoRepository.findById(d.productoId())
                        .orElseThrow(() -> new RuntimeException("Producto no existe"));

                if (ventaProduccion) {
                    descontarInventarioProduccion(producto, sede, d.cantidad(), vendedor, cliente);
                } else {
                    procesarDescuentoInventarioGeneral(producto, sede, d.cantidad());
                }

                Double precioUnitario = producto.getPrecioVenta();
                if (cliente != null) {
                    Optional<PrecioClienteProducto> precioCliente = precioClienteProductoRepository
                            .findByClienteIdAndProductoCodigo(cliente.getId(), producto.getCodigo());
                    if (precioCliente.isPresent() && Boolean.TRUE.equals(precioCliente.get().getActivo())) {
                        precioUnitario = precioCliente.get().getPrecioVenta();
                    }
                }

                detalle.setProducto(producto);
                detalle.setPrecioUnitario(precioUnitario);
                detalle.setSubtotal(precioUnitario * d.cantidad());
            }

            else {
                if (ventaProduccion) {
                    throw new RuntimeException("Produccion solo puede despachar productos del catalogo");
                }

                if (d.nombreLibre() == null || d.precioUnitario() == null) {
                    throw new RuntimeException("Producto rapido invalido");
                }

                detalle.setNombreLibre(d.nombreLibre());
                detalle.setPrecioUnitario(d.precioUnitario());
                detalle.setSubtotal(d.precioUnitario() * d.cantidad());
            }

            detalles.add(detalle);
            total += detalle.getSubtotal();
        }

        venta.setDetalles(detalles);
        venta.setTotal(total);

        return ventaRepository.save(venta);
    }

    private void procesarDescuentoInventarioGeneral(Producto producto, Sede sede, Integer cantidad) {
        if (!producto.getMateriasPrimas().isEmpty()) {

            for (ProductoMateriaPrima pmp : producto.getMateriasPrimas()) {

                MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                        .findByMateriaPrimaCodigoAndSedeId(
                                pmp.getMateriaPrima().getCodigo(),
                                sede.getId())
                        .orElseThrow(() -> new RuntimeException(
                                "No hay " + pmp.getMateriaPrima().getNombre() + " en esta sede"
                        ));

                double mlNecesarios = pmp.getMlConsumidos() * cantidad;

                if (mpSede.getCantidadActualMl() < mlNecesarios) {
                    throw new RuntimeException(
                            "Materia prima insuficiente: " + pmp.getMateriaPrima().getNombre()
                    );
                }

                mpSede.setCantidadActualMl(mpSede.getCantidadActualMl() - mlNecesarios);
                materiaPrimaSedeRepository.save(mpSede);
            }

            inventarioRepository.findByProductoCodigoAndSedeId(producto.getCodigo(), sede.getId())
                    .ifPresent(inventario -> notificacionStockMinimoService.evaluarYNotificar(
                            inventario,
                            calcularStockDisponibleDesdeMateriaPrima(producto, sede)
                    ));

        } else {

            Inventario inventario = inventarioRepository
                    .findByProductoCodigoAndSedeId(producto.getCodigo(), sede.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "No hay inventario para " + producto.getNombre()
                    ));

            if (inventario.getStockActual() < cantidad) {
                throw new RuntimeException(
                        "Stock insuficiente para " + producto.getNombre()
                );
            }

            inventario.setStockActual(inventario.getStockActual() - cantidad);
            inventarioRepository.save(inventario);
            notificacionStockMinimoService.evaluarYNotificar(inventario, inventario.getStockActual());
        }

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setSede(sede);
        movimiento.setTipo(TipoMovimiento.SALIDA);
        movimiento.setCantidad(cantidad);
        movimiento.setObservacion("Venta de producto");
        movimiento.setFecha(
                ZonedDateTime.now(ZoneId.of("America/Bogota"))
                        .toLocalDateTime()
        );

        movimientoInventarioRepository.save(movimiento);
    }

    private void descontarInventarioProduccion(
            Producto producto,
            Sede sede,
            Integer cantidad,
            Vendedor vendedor,
            Cliente cliente
    ) {
        InventarioProduccion inventario = inventarioProduccionRepository
                .findByProductoCodigoAndSedeId(producto.getCodigo(), sede.getId())
                .orElseThrow(() -> new RuntimeException(
                        "Inventario de produccion insuficiente para " + producto.getNombre()
                ));

        if (inventario.getStockActual() < cantidad) {
            throw new RuntimeException("Stock de produccion insuficiente para " + producto.getNombre());
        }

        inventario.setStockActual(inventario.getStockActual() - cantidad);
        inventario.setDespachadoAcumulado(inventario.getDespachadoAcumulado() + cantidad);
        inventarioProduccionRepository.save(inventario);

        MovimientoProduccion movimiento = new MovimientoProduccion();
        movimiento.setProducto(producto);
        movimiento.setSede(sede);
        movimiento.setCliente(cliente);
        movimiento.setVendedor(vendedor);
        movimiento.setTipo(TipoMovimientoProduccion.DESPACHO);
        movimiento.setCantidad(cantidad);
        movimiento.setObservacion("Despacho por venta");

        movimientoProduccionRepository.save(movimiento);
    }

    @Override
    @Transactional
    public List<VentaResponseDTO> listarVentasPorVendedor(Long vendedorId) {
        return ventaRepository.findByVendedorCodigo(vendedorId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<VentaResponseDTO> listarVentasPorVendedorEntreFechas(
            Long vendedorId,
            LocalDateTime desde,
            LocalDateTime hasta
    ) {
        return ventaRepository
                .findByVendedorCodigoAndFechaBetween(vendedorId, desde, hasta)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<VentaResponseDTO> listarVentasPorCorreoVendedor(String correoVendedor) {
        return ventaRepository.findByVendedorCorreoOrderByFechaDesc(correoVendedor)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<VentaResponseDTO> listarVentasPorCorreoVendedorEntreFechas(
            String correoVendedor,
            LocalDateTime desde,
            LocalDateTime hasta
    ) {
        return ventaRepository
                .findByVendedorCorreoAndFechaBetweenOrderByFechaDesc(correoVendedor, desde, hasta)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<VentaResponseDTO> listarVentasPorSede(Long sedeId) {
        return ventaRepository.findBySedeId(sedeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<VentaResponseDTO> listarVentasPorSedeEntreFechas(
            Long sedeId,
            LocalDateTime desde,
            LocalDateTime hasta
    ) {
        return ventaRepository
                .findBySedeIdAndFechaBetween(sedeId, desde, hasta)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public VentaResponseDTO mapToResponse(Venta venta) {
        String nombreUsuario;
        if (venta.getVendedor() != null) {
            nombreUsuario = venta.getVendedor().getNombre();
        } else if (venta.getAdministrador() != null) {
            nombreUsuario = venta.getAdministrador().getNombre();
        } else {
            nombreUsuario = "Usuario desconocido";
        }

        return new VentaResponseDTO(
                venta.getId(),
                venta.getFecha(),
                venta.getTotal(),
                nombreUsuario,
                venta.getSede().getUbicacion(),
                venta.getCliente() != null ? venta.getCliente().getId() : null,
                venta.getCliente() != null ? venta.getCliente().getNombre() : null,
                venta.getAnulado(),
                !Boolean.TRUE.equals(venta.getAnulado()),
                venta.getDetalles().stream()
                        .map(d -> new DetalleVentaResponseDTO(
                                d.getProducto() != null ? d.getProducto().getCodigo() : null,
                                d.getProducto() != null ? d.getProducto().getNombre() : d.getNombreLibre(),
                                d.getCantidad(),
                                d.getPrecioUnitario(),
                                d.getSubtotal(),
                                d.getNombreLibre()
                        ))
                        .toList()
        );
    }

    @Override
    @Transactional
    public void anularVenta(Long ventaId) {

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if (venta.getAnulado()) {
            throw new RuntimeException("La venta ya esta anulada");
        }

        venta.setAnulado(true);

        ventaRepository.save(venta);
    }


    @Override
    @Transactional
    public void cambiarEstadoVenta(Long ventaId, Boolean valido, Long empresaNit) {
        if (valido == null) {
            throw new RuntimeException("El estado de la venta es obligatorio");
        }

        Venta venta = ventaRepository.findByIdAndSedeEmpresaNit(ventaId, empresaNit)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada para la empresa"));

        venta.setAnulado(!valido);
        ventaRepository.save(venta);
    }

    @Override
    @Transactional
    public List<VentaResponseDTO> listarVentasAnuladas(Long sedeId) {
        return ventaRepository
                .findBySedeIdAndAnuladoTrue(sedeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private int calcularStockDisponibleDesdeMateriaPrima(Producto producto, Sede sede) {
        int stock = Integer.MAX_VALUE;

        for (ProductoMateriaPrima pmp : producto.getMateriasPrimas()) {
            MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                    .findByMateriaPrimaCodigoAndSedeId(pmp.getMateriaPrima().getCodigo(), sede.getId())
                    .orElse(null);

            if (mpSede == null) {
                return 0;
            }

            int unidades = (int) Math.floor(mpSede.getCantidadActualMl() / pmp.getMlConsumidos());
            stock = Math.min(stock, unidades);
        }

        return stock == Integer.MAX_VALUE ? 0 : stock;
    }

    private boolean esVendedorProduccion(Vendedor vendedor) {
        return vendedor != null && vendedor.getTipoPerfil() == TipoPerfilVendedor.PRODUCCION;
    }

    private Empresa obtenerEmpresaVendedor(Vendedor vendedor) {
        if (vendedor.getEmpresa() != null) {
            return vendedor.getEmpresa();
        }

        if (vendedor.getSede() != null && vendedor.getSede().getEmpresa() != null) {
            return vendedor.getSede().getEmpresa();
        }

        throw new RuntimeException("El vendedor no tiene empresa asociada");
    }

    private Sede obtenerSedeDesdeVendedor(Vendedor vendedor) {
        if (vendedor.getSede() == null) {
            throw new RuntimeException("El perfil de produccion no tiene sede asociada");
        }
        return vendedor.getSede();
    }
}




