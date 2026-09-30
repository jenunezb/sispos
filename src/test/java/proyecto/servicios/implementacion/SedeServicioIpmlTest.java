package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.SedeCrearDTO;
import proyecto.entidades.Empresa;
import proyecto.entidades.Sede;
import proyecto.repositorios.EmpresaRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.SuscripcionSedeRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SedeServicioIpmlTest {

    @Mock
    private SedeRepository sedeRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private SuscripcionSedeInicializacionService suscripcionSedeInicializacionService;

    @Mock
    private SuscripcionSedeRepository suscripcionSedeRepository;

    @InjectMocks
    private SedeServicioIpml servicio;

    @Test
    void crearDebeDejarLaNuevaSedePendienteDeActivacion() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        when(sedeRepository.existsByUbicacionIgnoreCase("Nueva sede")).thenReturn(false);
        when(empresaRepository.findById(900123456L)).thenReturn(Optional.of(empresa));
        when(sedeRepository.save(any(Sede.class))).thenAnswer(invocacion -> {
            Sede sede = invocacion.getArgument(0);
            sede.setId(30L);
            return sede;
        });

        var respuesta = servicio.crear(
                new SedeCrearDTO("Nueva sede", "Centro"),
                900123456L
        );

        assertEquals(30L, respuesta.id());
        assertEquals("Centro", respuesta.ubicacion());
        assertEquals("SIN_CONFIGURAR", respuesta.estadoSuscripcion());
        verify(suscripcionSedeInicializacionService).crearPendienteActivacion(any(Sede.class));
    }
}
