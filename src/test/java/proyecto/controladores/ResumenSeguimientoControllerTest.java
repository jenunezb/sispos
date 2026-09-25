package proyecto.controladores;

import org.junit.jupiter.api.Test;
import proyecto.entidades.Administrador;
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.servicios.implementacion.ResumenSeguimientoService;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResumenSeguimientoControllerTest {
    @Test void validaLaSedeAntesDeConsultar() {
        var acceso = mock(AdministradorAccesoService.class);
        var servicio = mock(ResumenSeguimientoService.class);
        var admin = new Administrador();
        var corte = LocalDateTime.of(2026, 9, 25, 12, 0);
        var resultado = new ResumenSeguimientoService.Resumen(corte.toLocalDate(), corte, List.of());
        when(acceso.obtenerAdministradorAutenticado("Bearer prueba")).thenReturn(admin);
        when(servicio.hoy(6L)).thenReturn(resultado);
        assertSame(resultado, new ResumenSeguimientoController(acceso, servicio).hoy("Bearer prueba", 6L));
        var orden = inOrder(acceso, servicio);
        orden.verify(acceso).obtenerAdministradorAutenticado("Bearer prueba");
        orden.verify(acceso).validarAccesoASede(admin, 6L);
        orden.verify(servicio).hoy(6L);
    }

    @Test void noConsultaDatosDeUnaSedeSinPermiso() {
        var acceso = mock(AdministradorAccesoService.class);
        var servicio = mock(ResumenSeguimientoService.class);
        var admin = new Administrador();
        when(acceso.obtenerAdministradorAutenticado("Bearer prueba")).thenReturn(admin);
        doThrow(new RuntimeException("Sin acceso")).when(acceso).validarAccesoASede(admin, 1L);
        assertThrows(RuntimeException.class,
                () -> new ResumenSeguimientoController(acceso, servicio).hoy("Bearer prueba", 1L));
        verifyNoInteractions(servicio);
    }
}
