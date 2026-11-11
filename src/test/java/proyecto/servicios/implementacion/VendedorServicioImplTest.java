package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.VendedorDTO;
import proyecto.entidades.CajaTurno;
import proyecto.entidades.EstadoCaja;
import proyecto.entidades.ModoPago;
import proyecto.entidades.Sede;
import proyecto.entidades.TipoPerfilVendedor;
import proyecto.entidades.Vendedor;
import proyecto.entidades.Venta;
import proyecto.repositorios.CajaTurnoRepository;
import proyecto.repositorios.GastoDiarioRepository;
import proyecto.repositorios.MovimientoProduccionRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.repositorios.VentaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendedorServicioImplTest {

    @Mock
    private VendedorRepository vendedorRepository;

    @Mock
    private SedeRepository sedeRepository;

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private MovimientoProduccionRepository movimientoProduccionRepository;

    @Mock
    private GastoDiarioRepository gastoDiarioRepository;

    @Mock
    private SuscripcionFeatureService suscripcionFeatureService;

    @Mock
    private CajaTurnoRepository cajaTurnoRepository;

    @InjectMocks
    private VendedorServicioImpl vendedorServicio;

    @Test
    void listarVendedoresDebeSoportarCiudadNula() {
        Vendedor vendedor = new Vendedor();
        vendedor.setCodigo(1);
        vendedor.setNombre("Juan");
        vendedor.setCedula("123");
        vendedor.setCorreo("juan@correo.com");
        vendedor.setTelefono("3001234567");
        vendedor.setEstado(true);
        vendedor.setTipoPerfil(TipoPerfilVendedor.PRODUCCION);
        vendedor.setCiudad(null);

        when(vendedorRepository.findVisiblesByEmpresaNit(900123456L)).thenReturn(List.of(vendedor));

        List<VendedorDTO> respuesta = vendedorServicio.listarVendedores(900123456L);

        assertEquals(1, respuesta.size());
        assertEquals("SIN CIUDAD", respuesta.get(0).ciudad());
        assertEquals("PRODUCCION", respuesta.get(0).perfil());
    }

    @Test
    void balanceActualDelVendedorUsaTodoElTurnoNocturno() {
        LocalDateTime apertura = LocalDateTime.of(2026, 8, 15, 19, 30);
        Sede sede = new Sede();
        sede.setId(3L);
        sede.setUbicacion("Centro");
        Vendedor vendedor = new Vendedor();
        vendedor.setCorreo("noche@empresa.com");
        vendedor.setSede(sede);
        CajaTurno caja = new CajaTurno();
        caja.setSede(sede);
        caja.setEstado(EstadoCaja.ABIERTA);
        caja.setFechaApertura(apertura);
        Venta ventaAntesDeMedianoche = new Venta();
        ventaAntesDeMedianoche.setTotal(40000.0);
        ventaAntesDeMedianoche.setModoPago(ModoPago.EFECTIVO);
        Venta ventaDespuesDeMedianoche = new Venta();
        ventaDespuesDeMedianoche.setTotal(25000.0);
        ventaDespuesDeMedianoche.setModoPago(ModoPago.EFECTIVO);

        when(vendedorRepository.findByCorreo("noche@empresa.com")).thenReturn(Optional.of(vendedor));
        when(cajaTurnoRepository.findFirstBySedeIdAndEstadoOrderByFechaAperturaDesc(3L, EstadoCaja.ABIERTA))
                .thenReturn(Optional.of(caja));
        when(ventaRepository.findBySedeIdAndFechaBetween(eq(3L), eq(apertura), any(LocalDateTime.class)))
                .thenReturn(List.of(ventaAntesDeMedianoche, ventaDespuesDeMedianoche));
        when(sedeRepository.findById(3L)).thenReturn(Optional.of(sede));

        var balance = vendedorServicio.balanceTurnoActual("noche@empresa.com");

        assertEquals(65000.0, balance.totalVentas());
        assertEquals(65000.0, balance.ventasEfectivo());
        verify(ventaRepository).findBySedeIdAndFechaBetween(eq(3L), eq(apertura), any(LocalDateTime.class));
    }
}
