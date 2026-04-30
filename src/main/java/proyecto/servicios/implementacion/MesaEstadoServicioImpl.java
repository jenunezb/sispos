package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.InventarioDTO;
import proyecto.dto.MesaEstadoDTO;
import proyecto.dto.MesaEstadoItemDTO;
import proyecto.entidades.*;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.MesaEstadoRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.servicios.interfaces.MesaEstadoServicio;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MesaEstadoServicioImpl implements MesaEstadoServicio {

    private final MesaEstadoRepository mesaEstadoRepository;
    private final SedeRepository sedeRepository;
    private final AdministradorRepository administradorRepository;
    private final VendedorRepository vendedorRepository;
    private final AdministradorAccesoService administradorAccesoService;

    @Override
    @Transactional(readOnly = true)
    public List<MesaEstadoDTO> listarPorSede(String correo, String rol, Long sedeId) {
        validarAcceso(correo, rol, sedeId);
        return mesaEstadoRepository.findDetalleBySedeId(sedeId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public MesaEstadoDTO guardarMesa(String correo, String rol, Long sedeId, Long mesaId, MesaEstadoDTO dto) {
        validarAcceso(correo, rol, sedeId);

        if (!mesaId.equals(dto.id())) {
            throw new RuntimeException("El id de la mesa no coincide con la ruta");
        }

        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        MesaEstado mesaEstado = mesaEstadoRepository.findDetalleBySedeIdAndMesaReferenciaId(sedeId, mesaId)
                .orElseGet(MesaEstado::new);

        mesaEstado.setSede(sede);
        mesaEstado.setMesaReferenciaId(mesaId);
        mesaEstado.setNumero(dto.numero() != null ? dto.numero() : 0);
        mesaEstado.setNombre(limpiar(dto.nombre()));
        mesaEstado.setEstado(normalizarEstado(dto.estado(), dto.carrito()));
        mesaEstado.setFechaActualizacion(ahoraBogota());

        List<MesaEstadoItem> items = new ArrayList<>();
        for (MesaEstadoItemDTO itemDto : dto.carrito() == null ? List.<MesaEstadoItemDTO>of() : dto.carrito()) {
            MesaEstadoItem item = new MesaEstadoItem();
            item.setMesaEstado(mesaEstado);
            item.setNombreLibre(limpiar(itemDto.nombreLibre()));
            item.setPrecioUnitario(itemDto.precioUnitario() != null ? itemDto.precioUnitario() : 0D);
            item.setCantidad(itemDto.cantidad() != null ? itemDto.cantidad() : 0);
            item.setTotal(itemDto.total() != null ? itemDto.total() : 0D);

            if (itemDto.producto() != null) {
                item.setProductoId(itemDto.producto().productoId());
                item.setProductoNombre(itemDto.producto().productoNombre());
                item.setStockActual(itemDto.producto().stockActual());
                item.setEntradas(itemDto.producto().entradas());
                item.setSalidas(itemDto.producto().salidas());
                item.setPerdidas(itemDto.producto().perdidas());
                item.setStockMinimo(itemDto.producto().stockMinimo());
                item.setPrecioVenta(itemDto.producto().precioVenta());
            }

            items.add(item);
        }

        mesaEstado.getItems().clear();
        mesaEstado.getItems().addAll(items);

        return mapToDto(mesaEstadoRepository.save(mesaEstado));
    }

    private MesaEstadoDTO mapToDto(MesaEstado mesaEstado) {
        return new MesaEstadoDTO(
                mesaEstado.getMesaReferenciaId(),
                mesaEstado.getNumero(),
                mesaEstado.getEstado(),
                mesaEstado.getItems().stream().map(item -> new MesaEstadoItemDTO(
                        item.getProductoId() != null || item.getProductoNombre() != null
                                ? new InventarioDTO(
                                null,
                                item.getProductoId(),
                                item.getProductoNombre(),
                                item.getStockActual(),
                                item.getEntradas(),
                                item.getSalidas(),
                                item.getPerdidas(),
                                item.getStockMinimo(),
                                item.getPrecioVenta()
                        )
                                : null,
                        item.getNombreLibre(),
                        item.getPrecioUnitario(),
                        item.getCantidad(),
                        item.getTotal()
                )).toList(),
                mesaEstado.getNombre()
        );
    }

    private void validarAcceso(String correo, String rol, Long sedeId) {
        if ("administrador".equalsIgnoreCase(rol)) {
            Administrador admin = administradorRepository.findByCorreoIgnoreCase(correo)
                    .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));
            administradorAccesoService.validarAccesoASede(admin, sedeId);
            return;
        }

        if ("vendedor".equalsIgnoreCase(rol) || "produccion".equalsIgnoreCase(rol)) {
            Vendedor vendedor = vendedorRepository.findByCorreoIgnoreCase(correo)
                    .orElseThrow(() -> new RuntimeException("Vendedor no encontrado"));

            if (vendedor.getSede() == null || !sedeId.equals(vendedor.getSede().getId())) {
                throw new RuntimeException("No tiene permisos para acceder a la sede seleccionada");
            }
            return;
        }

        throw new RuntimeException("No tiene permisos para acceder a este recurso");
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private String normalizarEstado(String estado, List<MesaEstadoItemDTO> carrito) {
        boolean ocupada = carrito != null && !carrito.isEmpty();
        if (ocupada) {
            return "OCUPADA";
        }
        return "LIBRE";
    }

    private LocalDateTime ahoraBogota() {
        return ZonedDateTime.now(ZoneId.of("America/Bogota")).toLocalDateTime();
    }
}
