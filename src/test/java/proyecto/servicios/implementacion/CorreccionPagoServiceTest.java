package proyecto.servicios.implementacion;

import io.jsonwebtoken.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import proyecto.dto.CorregirPagoDTO;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.servicios.interfaces.VentaServicio;
import proyecto.utils.JWTUtils;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorreccionPagoServiceTest {
    VentaAccesoService acceso = mock(VentaAccesoService.class);
    JWTUtils jwt = mock(JWTUtils.class);
    VentaRepository ventas = mock(VentaRepository.class);
    CajaTurnoRepository cajas = mock(CajaTurnoRepository.class);
    CorreccionPagoVentaRepository historial = mock(CorreccionPagoVentaRepository.class);
    VentaServicio mapper = mock(VentaServicio.class);
    CorreccionPagoService service = new CorreccionPagoService(acceso, jwt, ventas, cajas, historial, mapper);
    Venta venta;
    CajaTurno caja;
    Claims claims;
    CorregirPagoDTO dto = new CorregirPagoDTO(ModoPago.EFECTIVO, ModoPago.TRANSFERENCIA, "Error al seleccionar");

    @BeforeEach @SuppressWarnings("unchecked")
    void preparar() {
        claims = Jwts.claims(); claims.setSubject("vendedor@test.com"); claims.put("rol", "vendedor");
        Jws<Claims> token = mock(Jws.class);
        when(jwt.parseJwt("token")).thenReturn(token); when(token.getBody()).thenReturn(claims);
        Sede sede = new Sede(); sede.setId(3L);
        Vendedor vendedor = new Vendedor(); vendedor.setCorreo("vendedor@test.com");
        venta = new Venta(); venta.setId(1L); venta.setSede(sede); venta.setVendedor(vendedor);
        venta.setFecha(LocalDateTime.of(2026, 9, 30, 10, 0)); venta.setTotal(5000.0);
        venta.setModoPago(ModoPago.EFECTIVO); venta.setMontoEfectivo(5000.0); venta.setMontoTransferencia(0.0);
        caja = new CajaTurno(); caja.setId(2L); caja.setEstado(EstadoCaja.ABIERTA);
        caja.setFechaApertura(venta.getFecha().minusHours(1));
        when(ventas.bloquearPorId(1L)).thenReturn(Optional.of(venta));
        when(cajas.bloquearTurnosDeVenta(3L, venta.getFecha())).thenReturn(List.of(caja));
    }

    @Test void corrigeSoloPagoYAudita() {
        service.corregir("Bearer token", 1L, dto);
        assertEquals(ModoPago.TRANSFERENCIA, venta.getModoPago());
        assertEquals(0.0, venta.getMontoEfectivo()); assertEquals(5000.0, venta.getMontoTransferencia());
        assertEquals(5000.0, venta.getTotal()); assertFalse(venta.getAnulado());
        verify(historial).save(argThat(r -> r.getAnterior() == ModoPago.EFECTIVO
                && r.getNuevo() == ModoPago.TRANSFERENCIA && r.getUsuario().equals("vendedor@test.com")
                && r.getMotivo().equals(dto.motivo()) && r.getEfectivoAnterior() == 5000.0));
        verify(acceso).validarVenta("Bearer token", 1L);
    }

    @Test void vendedorNoCorrigeTurnoCerrado() {
        caja.setEstado(EstadoCaja.CERRADA);
        assertThrows(IllegalArgumentException.class, () -> service.corregir("Bearer token", 1L, dto));
        verifyNoInteractions(historial); verify(ventas, never()).saveAndFlush(any());
    }

    @Test void vendedorNoCorrigeSinTurno() {
        when(cajas.bloquearTurnosDeVenta(any(), any())).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class, () -> service.corregir("Bearer token", 1L, dto));
    }

    @Test void vendedorNoCorrigeVentaAjena() {
        venta.getVendedor().setCorreo("otro@test.com");
        assertThrows(IllegalArgumentException.class, () -> service.corregir("Bearer token", 1L, dto));
    }

    @Test void adminRecalculaCierreSinCambiarContado() {
        claims.put("rol", "administrador"); caja.setEstado(EstadoCaja.CERRADA);
        caja.setFechaCierre(venta.getFecha().plusHours(1)); caja.setBaseInicial(1000.0);
        caja.setGastosEfectivo(200.0); caja.setEfectivoContado(800.0);
        when(ventas.totalVentasEfectivoPorSedeEntreFechas(any(), any(), any())).thenReturn(0.0);
        service.corregir("Bearer token", 1L, dto);
        assertEquals(800.0, caja.getEfectivoEsperado()); assertEquals(800.0, caja.getEfectivoContado());
        assertEquals(0.0, caja.getDiferencia()); verify(cajas).save(caja);
    }

    @Test void rechazaPagoDesactualizado() {
        venta.setModoPago(ModoPago.TRANSFERENCIA);
        assertThrows(IllegalArgumentException.class, () -> service.corregir("Bearer token", 1L, dto));
        verifyNoInteractions(historial);
    }

    @Test void rechazaMixtoAnuladaYMotivoVacio() {
        assertThrows(IllegalArgumentException.class, () -> service.corregir("Bearer token", 1L,
                new CorregirPagoDTO(ModoPago.EFECTIVO, ModoPago.MIXTO, "Motivo")));
        assertThrows(IllegalArgumentException.class, () -> service.corregir("Bearer token", 1L,
                new CorregirPagoDTO(ModoPago.EFECTIVO, ModoPago.TRANSFERENCIA, "  ")));
        venta.setAnulado(true);
        assertThrows(IllegalArgumentException.class, () -> service.corregir("Bearer token", 1L, dto));
        verifyNoInteractions(historial);
    }

    @Test void accesoAjenoNoConsultaNiModifica() {
        doThrow(new IllegalArgumentException("Otra empresa")).when(acceso).validarVenta("Bearer token", 1L);
        assertThrows(IllegalArgumentException.class, () -> service.corregir("Bearer token", 1L, dto));
        assertThrows(IllegalArgumentException.class, () -> service.historial("Bearer token", 1L));
        verifyNoInteractions(ventas, historial);
    }

    @Test void produccionYCocinaNoPuedenCorregir() {
        for (String rol : List.of("produccion", "cocina")) {
            claims.put("rol", rol);
            assertThrows(IllegalArgumentException.class, () -> service.corregir("Bearer token", 1L, dto));
        }
        verifyNoInteractions(historial);
        verify(ventas, never()).saveAndFlush(any());
    }
}
