package proyecto.servicios.implementacion;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.DetalleVentaDTO;
import proyecto.dto.DetalleVentaResponseDTO;
import proyecto.dto.VentaRecuestDTO;
import proyecto.dto.VentaResponseDTO;
import proyecto.entidades.*;
import proyecto.repositorios.ProductoRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.repositorios.VentaRepository;
import proyecto.servicios.interfaces.InventarioServicio;
import proyecto.servicios.interfaces.VentaServicio;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VentaServicioImpl implements VentaServicio{

        private final VentaRepository ventaRepository;
        private final ProductoRepository productoRepository;
        private final VendedorRepository vendedorRepository;
        private final SedeRepository sedeRepository;
        private final InventarioServicio inventarioServicio;


        @Transactional
        public Venta crearVenta(VentaRecuestDTO dto) {

            Vendedor vendedor = vendedorRepository.findById(dto.vendedorId())
                    .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));

            Sede sede = sedeRepository.findById(dto.sedeId())
                    .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

            Venta venta = new Venta();
            venta.setFecha(ZonedDateTime.now(ZoneId.of("America/Bogota")).toLocalDateTime());
            venta.setVendedor(vendedor);
            venta.setSede(sede);
            venta.setModoPago(
                    dto.modoPago() != null ? dto.modoPago() : ModoPago.EFECTIVO
            );


            double total = 0;

            List<DetalleVenta> detalles = new ArrayList<>();

            venta = ventaRepository.save(venta);

            for (DetalleVentaDTO d : dto.detalles()) {

                Producto producto = productoRepository.findById(d.productoId())
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                double precio = producto.getPrecioVenta();
                double subtotal = precio * d.cantidad();

                DetalleVenta detalle = new DetalleVenta();
                detalle.setProducto(producto);
                detalle.setCantidad(d.cantidad());
                detalle.setPrecioUnitario(precio);
                detalle.setSubtotal(subtotal);
                detalle.setVenta(venta);

                detalles.add(detalle);
                total += subtotal;

                // 🔻 Descontar inventario
                inventarioServicio.registrarSalida(
                        producto.getCodigo(),
                        sede.getId(),
                        d.cantidad(),
                        "Venta #" + venta.getId()
                );
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
                .map(this::toResponseDTO)
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
                .map(this::toResponseDTO)
                .toList();
    }

    private VentaResponseDTO toResponseDTO(Venta venta) {
        return new VentaResponseDTO(
                venta.getId(),
                venta.getFecha(),
                venta.getTotal(),
                venta.getVendedor().getNombre(),
                venta.getSede().getNombre(),
                venta.getDetalles().stream()
                        .map(d -> new DetalleVentaResponseDTO(
                                d.getProducto().getCodigo(),
                                d.getProducto().getNombre(),
                                d.getCantidad(),
                                d.getPrecioUnitario(),
                                d.getSubtotal()
                        ))
                        .toList()
        );
    }

    @Override
    @Transactional
    public List<VentaResponseDTO> listarVentasPorSede(Long sedeId) {

        return ventaRepository.findBySedeId(sedeId)
                .stream()
                .map(this::toResponseDTO)
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
                .map(this::toResponseDTO)
                .toList();
    }

}