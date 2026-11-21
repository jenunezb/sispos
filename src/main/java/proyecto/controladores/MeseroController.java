package proyecto.controladores;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.MensajeDTO;
import proyecto.dto.MeseroDTO;
import proyecto.entidades.Mesero;
import proyecto.servicios.implementacion.MeseroCuentaService;
import proyecto.servicios.implementacion.MeseroPedidoService;

import java.util.List;

import static proyecto.utils.FiltroToken.ATRIBUTO_MESERO;

@RestController
@RequestMapping("/api/mesero")
@RequiredArgsConstructor
public class MeseroController {
    private final MeseroCuentaService cuentas;
    private final MeseroPedidoService pedidos;

    @PostMapping("/login")
    public MensajeDTO<MeseroDTO.Sesion> login(@Valid @RequestBody MeseroDTO.Login dto) {
        return new MensajeDTO<>(false, cuentas.login(dto));
    }

    @GetMapping("/mesas")
    public MensajeDTO<List<MeseroDTO.Mesa>> mesas(@RequestAttribute(ATRIBUTO_MESERO) Mesero mesero) {
        return new MensajeDTO<>(false, pedidos.listarMesas(mesero));
    }

    @GetMapping("/productos")
    public MensajeDTO<List<MeseroDTO.Producto>> productos(@RequestAttribute(ATRIBUTO_MESERO) Mesero mesero) {
        return new MensajeDTO<>(false, pedidos.listarProductos(mesero));
    }

    @PostMapping("/mesas/{mesaId}/pedidos")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeDTO<MeseroDTO.PedidoGuardado> guardar(
            @RequestAttribute(ATRIBUTO_MESERO) Mesero mesero, @PathVariable Long mesaId,
            @Valid @RequestBody MeseroDTO.Pedido dto) {
        return new MensajeDTO<>(false, pedidos.guardarPedido(mesero, mesaId, dto));
    }
}
