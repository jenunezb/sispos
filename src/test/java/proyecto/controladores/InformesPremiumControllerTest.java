package proyecto.controladores;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import proyecto.entidades.*;
import proyecto.excepciones.GlobalExceptionHandler;
import proyecto.repositorios.SedeRepository;
import proyecto.servicios.implementacion.*;
import proyecto.servicios.interfaces.ReporteVentasAdminServicio;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InformesPremiumControllerTest {
    @Test void seguimientoResponde403SinConsultarDatos() throws Exception {
        var acceso = mock(AdministradorAccesoService.class);
        var resumen = mock(ResumenSeguimientoService.class);
        var planes = mock(SuscripcionFeatureService.class);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Requiere PREMIUM"))
                .when(planes).validarInformesHabilitados(6L);
        MockMvcBuilders.standaloneSetup(new ResumenSeguimientoController(acceso, resumen, planes))
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/api/inventarios/seguimiento/6/hoy").header("Authorization", "Bearer prueba"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("respuesta").value("Requiere PREMIUM"));
        verifyNoInteractions(resumen);
    }
    @Test void reportesPorSedeResponden403SinConsultarDatos() throws Exception {
        var acceso = mock(AdministradorAccesoService.class);
        var reportes = mock(ReporteVentasAdminServicio.class);
        var planes = mock(SuscripcionFeatureService.class);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Requiere PREMIUM"))
                .when(planes).validarInformesHabilitados(6L);
        MockMvcBuilders.standaloneSetup(new ReporteVentasAdminController(reportes, acceso,
                        mock(SedeRepository.class), planes))
                .setControllerAdvice(new GlobalExceptionHandler()).build()
                .perform(get("/api/administrador/reportes/ventas/por-dia")
                        .header("Authorization", "Bearer prueba").param("sedeId", "6")
                        .param("desde", "2026-11-01").param("hasta", "2026-11-11"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reportes);
    }
    @Test void consolidadoIncluyeSoloSedesVisiblesPremium() {
        var acceso = mock(AdministradorAccesoService.class);
        var reportes = mock(ReporteVentasAdminServicio.class);
        var planes = mock(SuscripcionFeatureService.class);
        var sedes = mock(SedeRepository.class);
        var admin = new Administrador();
        var premium = new Sede(); premium.setId(6L);
        var basica = new Sede(); basica.setId(7L);
        when(acceso.obtenerAdministradorAutenticado("Bearer prueba")).thenReturn(admin);
        when(acceso.resolverEmpresaNit(admin, null)).thenReturn(123L);
        when(acceso.obtenerSedesVisibles(admin)).thenReturn(List.of(premium, basica));
        when(sedes.findByEmpresaNitAndIdIn(123L, List.of(6L, 7L))).thenReturn(List.of(premium, basica));
        when(planes.tieneInformesHabilitados(6L)).thenReturn(true);
        var desde = LocalDate.of(2026, 11, 1); var hasta = desde.plusDays(10);
        new ReporteVentasAdminController(reportes, acceso, sedes, planes)
                .ventasPorDia("Bearer prueba", null, null, desde, hasta);
        verify(reportes).obtenerVentasPorDia(List.of(6L), desde, hasta);
    }
}
