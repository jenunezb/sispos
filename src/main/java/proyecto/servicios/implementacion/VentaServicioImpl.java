package proyecto.servicios.implementacion;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.*;
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

    @Transactional
    public Venta crearVenta(VentaRecuestDTO dto) {

        // ===============================
        // 🔹 VALIDAR USUARIO (VENDEDOR O ADMIN)
        // ===============================

        Optional<Vendedor> vendedorOpt = vendedorRepository.findByCorreo(dto.correo());

        Vendedor vendedor = null;
        Administrador administrador = null;

        if (vendedorOpt.isPresent()) {

            vendedor = vendedorOpt.get();

        } else {

            administrador = administradorRepository
                    .findByCorreo(dto.correo())
                    .orElseThrow(() -> new RuntimeException("Usuario no autorizado"));
        }

        // ===============================
        // 🔹 VALIDAR SEDE
        // ===============================

        Sede sede = sedeRepository.findById(dto.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        // ===============================
        // 🔹 CREAR VENTA
        // ===============================

        Venta venta = new Venta();
        venta.setFecha(ZonedDateTime.now(ZoneId.of("America/Bogota")).toLocalDateTime());

        // 🔥 AQUÍ ESTÁ LO IMPORTANTE
        venta.setVendedor(vendedor);
        venta.setAdministrador(administrador);

        venta.setSede(sede);
        venta.setModoPago(dto.modoPago() != null ? dto.modoPago() : ModoPago.EFECTIVO);

        double total = 0;
        List<DetalleVenta> detalles = new ArrayList<>();

        // ===============================
        // 🔹 PROCESAR DETALLES
        // ===============================

        for (DetalleVentaDTO d : dto.detalles()) {

            DetalleVenta detalle = new DetalleVenta();
            detalle.setCantidad(d.cantidad());
            detalle.setVenta(venta);

            if (d.productoId() != null) {

                Producto producto = productoRepository.findById(d.productoId())
                        .orElseThrow(() -> new RuntimeException("Producto no existe"));

                if (!producto.getMateriasPrimas().isEmpty()) {

                    for (ProductoMateriaPrima pmp : producto.getMateriasPrimas()) {

                        MateriaPrimaSede mpSede = materiaPrimaSedeRepository
                                .findByMateriaPrimaCodigoAndSedeId(
                                        pmp.getMateriaPrima().getCodigo(),
                                        sede.getId())
                                .orElseThrow(() -> new RuntimeException(
                                        "No hay " + pmp.getMateriaPrima().getNombre() + " en esta sede"
                                ));

                        double mlNecesarios = pmp.getMlConsumidos() * d.cantidad();

                        if (mpSede.getCantidadActualMl() < mlNecesarios) {
                            throw new RuntimeException(
                                    "Materia prima insuficiente: " + pmp.getMateriaPrima().getNombre()
                            );
                        }

                        mpSede.setCantidadActualMl(mpSede.getCantidadActualMl() - mlNecesarios);
                        materiaPrimaSedeRepository.save(mpSede);
                    }

                } else {

                    Inventario inventario = inventarioRepository
                            .findByProductoCodigoAndSedeId(producto.getCodigo(), sede.getId())
                            .orElseThrow(() -> new RuntimeException(
                                    "No hay inventario para " + producto.getNombre()
                            ));

                    if (inventario.getStockActual() < d.cantidad()) {
                        throw new RuntimeException(
                                "Stock insuficiente para " + producto.getNombre()
                        );
                    }

                    inventario.setStockActual(inventario.getStockActual() - d.cantidad());
                    inventarioRepository.save(inventario);
                }

                MovimientoInventario movimiento = new MovimientoInventario();
                movimiento.setProducto(producto);
                movimiento.setSede(sede);
                movimiento.setTipo(TipoMovimiento.SALIDA);
                movimiento.setCantidad(d.cantidad());
                movimiento.setObservacion("Venta de producto");
                movimiento.setFecha(
                        ZonedDateTime.now(ZoneId.of("America/Bogota"))
                                .toLocalDateTime()
                );

                movimientoInventarioRepository.save(movimiento);

                detalle.setProducto(producto);
                detalle.setPrecioUnitario(producto.getPrecioVenta());
                detalle.setSubtotal(producto.getPrecioVenta() * d.cantidad());
            }

            else {

                if (d.nombreLibre() == null || d.precioUnitario() == null) {
                    throw new RuntimeException("Producto rápido inválido");
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

    // 🔹 Método helper para convertir a DTO de respuesta
    public VentaResponseDTO mapToResponse(Venta venta) {
        // Determinar el nombre del usuario que hizo la venta
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
                nombreUsuario,  // ahora puede ser vendedor o administrador
                venta.getSede().getUbicacion(),
                venta.getDetalles().stream()
                        .map(d -> new DetalleVentaResponseDTO(
                                d.getProducto() != null ? d.getProducto().getCodigo() : null,
                                d.getProducto() != null ? d.getProducto().getNombre() : d.getNombreLibre(),
                                d.getCantidad(),
                                d.getPrecioUnitario(),
                                d.getSubtotal(),
                                d.getNombreLibre() // solo para referencia, puede ser null si es normal
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
            throw new RuntimeException("La venta ya está anulada");
        }

        venta.setAnulado(true);

        ventaRepository.save(venta);
    }


    @Override
    @Transactional
    public void cambiarEstadoVenta(Long ventaId, Boolean valido, Long empresaNit) {
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

}
