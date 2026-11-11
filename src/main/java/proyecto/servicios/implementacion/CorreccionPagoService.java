package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.CorregirPagoDTO;
import proyecto.dto.VentaResponseDTO;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.servicios.interfaces.VentaServicio;
import proyecto.utils.JWTUtils;
import proyecto.utils.FechaColombiaUtils;
import java.util.List;

@Service @RequiredArgsConstructor
public class CorreccionPagoService {
    private final VentaAccesoService acceso;
    private final JWTUtils jwt;
    private final VentaRepository ventas;
    private final CajaTurnoRepository cajas;
    private final CorreccionPagoVentaRepository historial;
    private final VentaServicio ventaServicio;

    @Transactional(readOnly = true)
    public List<CorreccionPagoVenta> historial(String authorization, Long id) {
        acceso.validarVenta(authorization, id);
        return historial.findByVentaIdOrderByIdDesc(id);
    }

    @Transactional
    public VentaResponseDTO corregir(String authorization, Long id, CorregirPagoDTO dto) {
        acceso.validarVenta(authorization, id);
        var claims = jwt.parseJwt(authorization.replace("Bearer ", "")).getBody();
        String rol = claims.get("rol", String.class);
        boolean admin = "administrador".equals(rol);
        if (!admin && !"vendedor".equals(rol)) throw new IllegalArgumentException("Perfil no autorizado");
        Venta venta = ventas.bloquearPorId(id).orElseThrow(() -> new IllegalArgumentException("Venta no encontrada"));
        if (Boolean.TRUE.equals(venta.getAnulado())) throw new IllegalArgumentException("No se puede corregir una venta anulada");
        if (dto.motivo() == null || dto.motivo().isBlank() || dto.motivo().trim().length() > 500)
            throw new IllegalArgumentException("Escribe un motivo de hasta 500 caracteres");
        if (venta.getModoPago() != dto.modoPagoAnterior())
            throw new IllegalArgumentException("El pago cambió. Vuelve a abrir el detalle antes de corregirlo");
        if ((venta.getModoPago() != ModoPago.EFECTIVO && venta.getModoPago() != ModoPago.TRANSFERENCIA)
                || (dto.modoPago() != ModoPago.EFECTIVO && dto.modoPago() != ModoPago.TRANSFERENCIA))
            throw new IllegalArgumentException("Esta corrección solo permite efectivo y transferencia, no pagos mixtos");
        if (venta.getModoPago() == dto.modoPago()) throw new IllegalArgumentException("Selecciona un medio de pago diferente");
        var turnos = cajas.bloquearTurnosDeVenta(venta.getSede().getId(), venta.getFecha());
        if (turnos.size() > 1) throw new IllegalArgumentException("La venta coincide con varios turnos. Requiere revisión de soporte");
        CajaTurno caja = turnos.isEmpty() ? null : turnos.get(0);
        if (!admin && (caja == null || caja.getEstado() != EstadoCaja.ABIERTA))
            throw new IllegalArgumentException("Solo el administrador puede corregir ventas sin turno abierto");
        if (!admin && (venta.getVendedor() == null || !claims.getSubject().equalsIgnoreCase(venta.getVendedor().getCorreo())))
            throw new IllegalArgumentException("Solo puedes corregir tus propias ventas del turno abierto");

        CorreccionPagoVenta registro = new CorreccionPagoVenta();
        registro.setVentaId(id);
        registro.setCajaId(caja == null ? null : caja.getId());
        registro.setFecha(FechaColombiaUtils.ahora());
        registro.setUsuario(claims.getSubject());
        registro.setRol(rol);
        registro.setAnterior(venta.getModoPago());
        registro.setNuevo(dto.modoPago());
        registro.setMotivo(dto.motivo().trim());
        double anterior = venta.getMontoEfectivo() != null ? venta.getMontoEfectivo()
                : venta.getModoPago() == ModoPago.EFECTIVO ? venta.getTotal() : 0.0;
        registro.setEfectivoAnterior(anterior);
        registro.setTransferenciaAnterior(venta.getMontoTransferencia() != null ? venta.getMontoTransferencia()
                : venta.getModoPago() == ModoPago.TRANSFERENCIA ? venta.getTotal() : 0.0);
        venta.setModoPago(dto.modoPago());
        venta.setMontoEfectivo(dto.modoPago() == ModoPago.EFECTIVO ? venta.getTotal() : 0.0);
        venta.setMontoTransferencia(dto.modoPago() == ModoPago.TRANSFERENCIA ? venta.getTotal() : 0.0);
        registro.setEfectivoNuevo(venta.getMontoEfectivo());
        registro.setTransferenciaNueva(venta.getMontoTransferencia());
        ventas.saveAndFlush(venta);
        if (caja != null && caja.getEstado() == EstadoCaja.CERRADA) {
            double efectivo = ventas.totalVentasEfectivoPorSedeEntreFechas(venta.getSede().getId(), caja.getFechaApertura(), caja.getFechaCierre());
            caja.setVentasEfectivo(efectivo);
            caja.setEfectivoEsperado((caja.getBaseInicial() == null ? 0 : caja.getBaseInicial()) + efectivo
                    - (caja.getGastosEfectivo() == null ? 0 : caja.getGastosEfectivo()));
            caja.setDiferencia((caja.getEfectivoContado() == null ? 0 : caja.getEfectivoContado()) - caja.getEfectivoEsperado());
            cajas.save(caja);
        }
        historial.save(registro);
        return ventaServicio.mapToResponse(venta);
    }
}
