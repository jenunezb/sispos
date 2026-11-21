package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import proyecto.dto.ComandaCocinaDetalleDTO;
import proyecto.dto.InventarioDTO;
import proyecto.dto.MesaEstadoDTO;
import proyecto.dto.MesaEstadoItemDTO;
import proyecto.dto.MeseroDTO;
import proyecto.entidades.Mesero;
import proyecto.servicios.interfaces.InventarioServicio;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeseroPedidoService {
    private final MesaEstadoServicioImpl mesas;
    private final InventarioServicio inventario;
    private final ComandaCocinaServicioImpl cocina;

    @Transactional(readOnly = true)
    public List<MeseroDTO.Mesa> listarMesas(Mesero mesero) {
        return mesas.listarMesasMesero(mesero).stream().map(this::mesa).toList();
    }

    @Transactional(readOnly = true)
    public List<MeseroDTO.Producto> listarProductos(Mesero mesero) {
        return inventario.listarPorSede(mesero.getSede().getId()).stream()
                .map(p -> new MeseroDTO.Producto(p.productoId(), p.productoNombre(), p.precioVenta()))
                .toList();
    }

    @Transactional
    public MeseroDTO.PedidoGuardado guardarPedido(Mesero mesero, Long mesaId, MeseroDTO.Pedido pedido) {
        Map<Long, InventarioDTO> catalogo = inventario.listarPorSede(mesero.getSede().getId()).stream()
                .collect(Collectors.toMap(InventarioDTO::productoId, Function.identity()));
        if (pedido.productos().stream().map(MeseroDTO.Linea::productoId).distinct().count() != pedido.productos().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envía una sola cantidad por producto");
        }
        List<MesaEstadoItemDTO> items = pedido.productos().stream()
                .map(linea -> prepararItem(catalogo.get(linea.productoId()), linea.cantidad()))
                .toList();

        MeseroDTO.Mesa mesa = mesa(mesas.agregarPedidoMesero(mesero, mesaId, pedido.version(), items));
        Long comandaId = null;
        if (pedido.enviarCocina()) {
            List<ComandaCocinaDetalleDTO> detalles = items.stream()
                    .map(item -> new ComandaCocinaDetalleDTO(item.producto().productoNombre(), item.cantidad()))
                    .toList();
            comandaId = cocina.crearComandaMesero(mesero, mesa.nombre(), pedido.observaciones(), detalles).id();
        }
        return new MeseroDTO.PedidoGuardado(mesa, comandaId);
    }

    private MesaEstadoItemDTO prepararItem(InventarioDTO producto, int cantidad) {
        if (producto == null || producto.precioVenta() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un producto ya no está disponible en esta sede");
        }
        double precio = producto.precioVenta();
        return new MesaEstadoItemDTO(producto, null, precio, cantidad, precio * cantidad);
    }

    private MeseroDTO.Mesa mesa(MesaEstadoDTO estado) {
        String nombre = estado.nombre() == null || estado.nombre().isBlank() ? "Mesa " + estado.numero() : estado.nombre();
        List<MeseroDTO.Renglon> pedido = estado.carrito().stream()
                .map(item -> new MeseroDTO.Renglon(item.producto() != null ? item.producto().productoNombre() : item.nombreLibre(),
                        item.cantidad(), item.total()))
                .toList();
        return new MeseroDTO.Mesa(estado.id(), nombre, estado.estado(), estado.version(), pedido);
    }
}
