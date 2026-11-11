package proyecto.servicios.implementacion;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import java.time.LocalDate;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class InformesSuscripcionTest {
    @ParameterizedTest
    @CsvSource({"PREMIUM,true,10,true", "PREMIUM,true,3,true", "PREMIUM,true,0,true",
            "PREMIUM,true,-1,false", "PREMIUM,false,10,false", "BASICO,true,10,false"})
    void dtoYValidacionCoinciden(PlanSuscripcionSede plan, boolean activa, int dias, boolean permitido) {
        var repo = mock(SuscripcionSedeRepository.class);
        var sedes = mock(SedeRepository.class);
        Sede sede = new Sede(); sede.setId(6L);
        SuscripcionSede sus = new SuscripcionSede(); sus.setSede(sede); sus.setPlan(plan);
        sus.setActiva(activa); sus.setFechaProximoVencimiento(LocalDate.now().plusDays(dias));
        when(repo.findBySedeId(6L)).thenReturn(Optional.of(sus));
        when(sedes.findById(6L)).thenReturn(Optional.of(sede));
        var feature = new SuscripcionFeatureService(repo);
        var servicio = new SedeServicioIpml(sedes, mock(EmpresaRepository.class),
                mock(SuscripcionSedeInicializacionService.class), repo);
        assertEquals(permitido, feature.tieneInformesHabilitados(6L));
        assertEquals(permitido, servicio.obtenerPorId(6L).informesHabilitados());
        if (permitido) assertDoesNotThrow(() -> feature.validarInformesHabilitados(6L));
        else assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> feature.validarInformesHabilitados(6L));
    }
    @Test void sinSuscripcionNoPermiteInformes() {
        var repo = mock(SuscripcionSedeRepository.class);
        assertFalse(new SuscripcionFeatureService(repo).tieneInformesHabilitados(6L));
    }
}
