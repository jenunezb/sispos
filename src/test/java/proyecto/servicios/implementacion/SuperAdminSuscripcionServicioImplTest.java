package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.SuperAdminPagoSuscripcionDTO;
import proyecto.dto.SuperAdminRegistrarPagoSuscripcionDTO;
import proyecto.entidades.EstadoSuscripcionSede;
import proyecto.entidades.Sede;
import proyecto.entidades.SuscripcionSede;
import proyecto.entidades.TipoCobroSuscripcion;
import proyecto.repositorios.PagoSuscripcionSedeRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.SuscripcionSedeRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminSuscripcionServicioImplTest {

    @Mock
    private SuscripcionSedeRepository suscripcionSedeRepository;

    @Mock
    private PagoSuscripcionSedeRepository pagoSuscripcionSedeRepository;

    @Mock
    private SedeRepository sedeRepository;

    @Mock
    private SuscripcionFeatureService suscripcionFeatureService;

    @InjectMocks
    private SuperAdminSuscripcionServicioImpl servicio;

    @Test
    void registrarPagoMensualDebeMantenerElMismoDiaDeCorte() {
        Sede sede = new Sede();
        sede.setId(11L);

        SuscripcionSede suscripcion = new SuscripcionSede();
        suscripcion.setId(6L);
        suscripcion.setSede(sede);
        suscripcion.setActiva(true);
        suscripcion.setEstadoServicio(EstadoSuscripcionSede.ACTIVO);
        suscripcion.setTipoCobro(TipoCobroSuscripcion.MENSUAL);
        suscripcion.setPrecioMensual(110000D);
        suscripcion.setFechaProximoVencimiento(LocalDate.of(2026, 6, 20));

        when(suscripcionSedeRepository.findBySedeId(11L)).thenReturn(Optional.of(suscripcion));
        when(pagoSuscripcionSedeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(suscripcionSedeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SuperAdminPagoSuscripcionDTO respuesta = servicio.registrarPago(
                new SuperAdminRegistrarPagoSuscripcionDTO(
                        11L,
                        "MENSUAL",
                        null,
                        LocalDate.of(2026, 6, 15),
                        "transferencia",
                        null
                ),
                "admin@steelsoft.com"
        );

        assertEquals(LocalDate.of(2026, 6, 20), respuesta.periodoDesde());
        assertEquals(LocalDate.of(2026, 7, 20), respuesta.periodoHasta());
        assertEquals(LocalDate.of(2026, 7, 20), suscripcion.getFechaProximoVencimiento());

        ArgumentCaptor<SuscripcionSede> captor = ArgumentCaptor.forClass(SuscripcionSede.class);
        verify(suscripcionSedeRepository, times(1)).save(captor.capture());
        assertEquals(LocalDate.of(2026, 7, 20), captor.getValue().getFechaProximoVencimiento());
    }

    @Test
    void registrarPagoMensualDebeTomarLaFechaDePagoComoNuevoCorteSiYaEstabaVencida() {
        Sede sede = new Sede();
        sede.setId(11L);

        SuscripcionSede suscripcion = new SuscripcionSede();
        suscripcion.setId(6L);
        suscripcion.setSede(sede);
        suscripcion.setActiva(true);
        suscripcion.setEstadoServicio(EstadoSuscripcionSede.VENCIDO);
        suscripcion.setTipoCobro(TipoCobroSuscripcion.MENSUAL);
        suscripcion.setPrecioMensual(110000D);
        suscripcion.setFechaProximoVencimiento(LocalDate.of(2026, 6, 20));

        when(suscripcionSedeRepository.findBySedeId(11L)).thenReturn(Optional.of(suscripcion));
        when(pagoSuscripcionSedeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(suscripcionSedeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SuperAdminPagoSuscripcionDTO respuesta = servicio.registrarPago(
                new SuperAdminRegistrarPagoSuscripcionDTO(
                        11L,
                        "MENSUAL",
                        null,
                        LocalDate.of(2026, 6, 25),
                        "transferencia",
                        null
                ),
                "admin@steelsoft.com"
        );

        assertEquals(LocalDate.of(2026, 6, 25), respuesta.periodoDesde());
        assertEquals(LocalDate.of(2026, 7, 25), respuesta.periodoHasta());
        assertEquals(LocalDate.of(2026, 7, 25), suscripcion.getFechaProximoVencimiento());
    }

    @Test
    void registrarPagoAnualDebeMantenerElMismoDiaDeCorte() {
        Sede sede = new Sede();
        sede.setId(11L);

        SuscripcionSede suscripcion = new SuscripcionSede();
        suscripcion.setId(6L);
        suscripcion.setSede(sede);
        suscripcion.setActiva(true);
        suscripcion.setEstadoServicio(EstadoSuscripcionSede.ACTIVO);
        suscripcion.setTipoCobro(TipoCobroSuscripcion.ANUAL);
        suscripcion.setPrecioAnual(1200000D);
        suscripcion.setFechaProximoVencimiento(LocalDate.of(2026, 6, 20));

        when(suscripcionSedeRepository.findBySedeId(11L)).thenReturn(Optional.of(suscripcion));
        when(pagoSuscripcionSedeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(suscripcionSedeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SuperAdminPagoSuscripcionDTO respuesta = servicio.registrarPago(
                new SuperAdminRegistrarPagoSuscripcionDTO(
                        11L,
                        "ANUAL",
                        null,
                        LocalDate.of(2026, 6, 15),
                        "transferencia",
                        null
                ),
                "admin@steelsoft.com"
        );

        assertEquals(LocalDate.of(2026, 6, 20), respuesta.periodoDesde());
        assertEquals(LocalDate.of(2027, 6, 20), respuesta.periodoHasta());
        assertEquals(LocalDate.of(2027, 6, 20), suscripcion.getFechaProximoVencimiento());
    }
}
