package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.GastoDiarioCrearDTO;
import proyecto.entidades.Administrador;
import proyecto.entidades.GastoDiario;
import proyecto.entidades.ModoPago;
import proyecto.entidades.Sede;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.GastoDiarioRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.utils.FechaColombiaUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GastoDiarioServicioImplTest {

    @Mock
    private GastoDiarioRepository gastoDiarioRepository;

    @Mock
    private SedeRepository sedeRepository;

    @InjectMocks
    private GastoDiarioServicioImpl gastoDiarioServicio;

    @Test
    void crearConAdministradorDebeGuardarFechaEnZonaBogota() {
        Sede sede = new Sede();
        sede.setId(3L);

        Administrador administrador = new Administrador();
        administrador.setCodigo(7);
        administrador.setNombre("Ana");
        administrador.setApellido("Mesa");

        when(sedeRepository.findById(3L)).thenReturn(Optional.of(sede));
        when(gastoDiarioRepository.save(any(GastoDiario.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime antes = FechaColombiaUtils.ahora();
        gastoDiarioServicio.crear(administrador, new GastoDiarioCrearDTO(3L, "Hielo", 12000.0, ModoPago.EFECTIVO));
        LocalDateTime despues = FechaColombiaUtils.ahora();

        ArgumentCaptor<GastoDiario> captor = ArgumentCaptor.forClass(GastoDiario.class);
        verify(gastoDiarioRepository).save(captor.capture());

        assertFechaEntre(captor.getValue().getFecha(), antes, despues);
    }

    @Test
    void crearConVendedorDebeGuardarFechaEnZonaBogota() {
        Sede sede = new Sede();
        sede.setId(3L);

        Vendedor vendedor = new Vendedor();
        vendedor.setCodigo(9);
        vendedor.setNombre("Luis");

        when(sedeRepository.findById(3L)).thenReturn(Optional.of(sede));
        when(gastoDiarioRepository.save(any(GastoDiario.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime antes = FechaColombiaUtils.ahora();
        gastoDiarioServicio.crear(vendedor, new GastoDiarioCrearDTO(3L, "Queso", 18000.0, ModoPago.TRANSFERENCIA));
        LocalDateTime despues = FechaColombiaUtils.ahora();

        ArgumentCaptor<GastoDiario> captor = ArgumentCaptor.forClass(GastoDiario.class);
        verify(gastoDiarioRepository).save(captor.capture());

        assertFechaEntre(captor.getValue().getFecha(), antes, despues);
    }

    @Test
    void noDebePermitirGastoConPagoMixto() {
        Sede sede = new Sede();
        sede.setId(3L);

        Administrador administrador = new Administrador();
        administrador.setCodigo(7);

        when(sedeRepository.findById(3L)).thenReturn(Optional.of(sede));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                gastoDiarioServicio.crear(administrador, new GastoDiarioCrearDTO(3L, "Prueba", 1000.0, ModoPago.MIXTO)));

        assertEquals("El gasto diario no admite modo de pago mixto", exception.getMessage());
    }

    private void assertFechaEntre(LocalDateTime actual, LocalDateTime inicio, LocalDateTime fin) {
        assertTrue(!actual.isBefore(inicio) && !actual.isAfter(fin),
                () -> "Fecha fuera del rango esperado de Bogota. actual=" + actual + ", inicio=" + inicio + ", fin=" + fin);
        assertTrue(Duration.between(inicio, fin).abs().getSeconds() < 5,
                () -> "La ventana de validacion fue demasiado grande: " + Duration.between(inicio, fin));
    }
}
