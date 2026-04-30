package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.SedeDTO;
import proyecto.entidades.Sede;
import proyecto.entidades.SuscripcionSede;
import proyecto.repositorios.EmpresaRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.SuscripcionSedeRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SedeServicioIpmlTest {

    @Mock
    private SedeRepository sedeRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private SuscripcionSedeRepository suscripcionSedeRepository;

    @InjectMocks
    private SedeServicioIpml sedeServicio;

    @Test
    void listarDebeIncluirEstadoActivoYFechaProximoVencimiento() {
        Sede sede = new Sede();
        sede.setId(10L);
        sede.setUbicacion("Centro");

        SuscripcionSede suscripcion = new SuscripcionSede();
        suscripcion.setSede(sede);
        suscripcion.setActiva(true);
        suscripcion.setFechaProximoVencimiento(LocalDate.of(2026, 5, 15));

        when(suscripcionSedeRepository.findBySedeIdIn(List.of(10L))).thenReturn(List.of(suscripcion));

        List<SedeDTO> resultado = sedeServicio.listar(List.of(sede));

        assertEquals(1, resultado.size());
        assertEquals(true, resultado.get(0).activa());
        assertEquals(LocalDate.of(2026, 5, 15), resultado.get(0).fechaProximoVencimiento());
    }

    @Test
    void obtenerPorIdDebeRetornarValoresPorDefectoSiLaSedeNoTieneSuscripcion() {
        Sede sede = new Sede();
        sede.setId(20L);
        sede.setUbicacion("Norte");

        when(sedeRepository.findById(20L)).thenReturn(Optional.of(sede));
        when(suscripcionSedeRepository.findBySedeId(20L)).thenReturn(Optional.empty());

        SedeDTO resultado = sedeServicio.obtenerPorId(20L);

        assertFalse(resultado.activa());
        assertNull(resultado.fechaProximoVencimiento());
    }
}
