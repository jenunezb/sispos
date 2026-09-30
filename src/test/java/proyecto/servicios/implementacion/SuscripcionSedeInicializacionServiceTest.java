package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.entidades.EstadoSuscripcionSede;
import proyecto.entidades.Sede;
import proyecto.entidades.SuscripcionSede;
import proyecto.repositorios.SuscripcionSedeRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuscripcionSedeInicializacionServiceTest {

    @Mock
    private SuscripcionSedeRepository suscripcionSedeRepository;

    @InjectMocks
    private SuscripcionSedeInicializacionService servicio;

    @Test
    void debeCrearLaSedeVencidaYPendienteDeActivacion() {
        Sede sede = new Sede();
        sede.setId(25L);
        when(suscripcionSedeRepository.save(any(SuscripcionSede.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.crearPendienteActivacion(sede);

        ArgumentCaptor<SuscripcionSede> captor = ArgumentCaptor.forClass(SuscripcionSede.class);
        verify(suscripcionSedeRepository).save(captor.capture());

        SuscripcionSede guardada = captor.getValue();
        assertEquals(sede, guardada.getSede());
        assertTrue(guardada.getActiva());
        assertEquals(EstadoSuscripcionSede.VENCIDO, guardada.getEstadoServicio());
        assertEquals(LocalDate.now().minusDays(1), guardada.getFechaProximoVencimiento());
        assertEquals(
                SuscripcionSedeInicializacionService.OBSERVACION_PENDIENTE_ACTIVACION,
                guardada.getObservacion()
        );
    }

    @Test
    void debeCrearUnMesDePruebaParaLaPrimeraSedeDeLaEmpresa() {
        Sede sede = new Sede();
        sede.setId(26L);
        when(suscripcionSedeRepository.save(any(SuscripcionSede.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.crearPeriodoPrueba(sede);

        ArgumentCaptor<SuscripcionSede> captor = ArgumentCaptor.forClass(SuscripcionSede.class);
        verify(suscripcionSedeRepository).save(captor.capture());

        SuscripcionSede guardada = captor.getValue();
        assertEquals(sede, guardada.getSede());
        assertTrue(guardada.getActiva());
        assertEquals(EstadoSuscripcionSede.ACTIVO, guardada.getEstadoServicio());
        assertEquals(LocalDate.now(), guardada.getFechaInicioServicio());
        assertEquals(LocalDate.now().plusMonths(1), guardada.getFechaProximoVencimiento());
        assertEquals(
                SuscripcionSedeInicializacionService.OBSERVACION_PERIODO_PRUEBA,
                guardada.getObservacion()
        );
    }
}
