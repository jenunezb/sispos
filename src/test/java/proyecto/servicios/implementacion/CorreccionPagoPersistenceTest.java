package proyecto.servicios.implementacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.sql.init.mode=never", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"})
class CorreccionPagoPersistenceTest {
    @Autowired EntityManager em;
    @Autowired VentaRepository ventas;
    @Autowired CajaTurnoRepository cajas;
    @Autowired CorreccionPagoVentaRepository historial;

    @Test void bloqueoHistorialYSumasRespetanSedeYTotal() {
        Empresa empresa = new Empresa(); empresa.setNit(999456L); em.persist(empresa);
        Sede sede = new Sede(); sede.setEmpresa(empresa); em.persist(sede);
        Sede otra = new Sede(); otra.setEmpresa(empresa); em.persist(otra);
        LocalDateTime fecha = LocalDateTime.of(2026, 9, 30, 10, 0);
        CajaTurno caja = new CajaTurno(); caja.setSede(sede); caja.setFechaApertura(fecha.minusHours(1));
        caja.setEstado(EstadoCaja.ABIERTA); caja.setBaseInicial(1000.0); em.persist(caja);
        Venta venta = new Venta(); venta.setSede(sede); venta.setFecha(fecha); venta.setTotal(5000.0);
        venta.setNumeroConsecutivo(1L); venta.setModoPago(ModoPago.EFECTIVO);
        venta.setMontoEfectivo(5000.0); venta.setMontoTransferencia(0.0); em.persist(venta); em.flush();
        Long id = venta.getId(); em.clear();
        assertEquals(1, cajas.bloquearTurnosDeVenta(sede.getId(), fecha).size());
        assertTrue(cajas.bloquearTurnosDeVenta(otra.getId(), fecha).isEmpty());
        assertTrue(cajas.bloquearTurnosDeVenta(sede.getId(), fecha.minusDays(1)).isEmpty());
        assertTrue(cajas.bloquearPorId(caja.getId()).isPresent());
        Venta bloqueada = ventas.bloquearPorId(id).orElseThrow();
        bloqueada.setModoPago(ModoPago.TRANSFERENCIA); bloqueada.setMontoEfectivo(0.0);
        bloqueada.setMontoTransferencia(5000.0); ventas.saveAndFlush(bloqueada);
        assertEquals(0.0, ventas.totalVentasEfectivoPorSedeEntreFechas(sede.getId(), fecha.minusHours(1), fecha.plusHours(1)));
        CorreccionPagoVenta registro = new CorreccionPagoVenta(); registro.setVentaId(id);
        registro.setFecha(fecha); registro.setUsuario("QA"); registro.setRol("administrador");
        registro.setAnterior(ModoPago.EFECTIVO); registro.setNuevo(ModoPago.TRANSFERENCIA); registro.setMotivo("Error");
        historial.saveAndFlush(registro); em.clear();
        assertEquals(1, historial.findByVentaIdOrderByIdDesc(id).size());
        assertEquals(5000.0, ventas.findById(id).orElseThrow().getTotal());
    }
}
