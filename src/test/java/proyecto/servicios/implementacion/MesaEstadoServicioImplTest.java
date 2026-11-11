package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.InventarioDTO;
import proyecto.dto.MesaEstadoDTO;
import proyecto.dto.MesaEstadoItemDTO;
import proyecto.entidades.Administrador;
import proyecto.entidades.MesaEstado;
import proyecto.entidades.MesaEstadoItem;
import proyecto.entidades.Sede;
import proyecto.entidades.Vendedor;
import proyecto.excepciones.MesaVersionConflictException;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.MesaEstadoRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesaEstadoServicioImplTest {

    @Mock
    private MesaEstadoRepository mesaEstadoRepository;
    @Mock
    private SedeRepository sedeRepository;
    @Mock
    private AdministradorRepository administradorRepository;
    @Mock
    private VendedorRepository vendedorRepository;
    @Mock
    private AdministradorAccesoService administradorAccesoService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MesaEstadoServicioImpl mesaEstadoServicio;

    @Test
    void guardarMesaDebePersistirCarritoYMarcarOcupada() {
        Sede sede = new Sede();
        sede.setId(5L);

        Administrador admin = new Administrador();
        admin.setCorreo("admin@correo.com");

        MesaEstadoDTO dto = new MesaEstadoDTO(
                2L,
                1,
                "LIBRE",
                List.of(new MesaEstadoItemDTO(
                        new InventarioDTO(null, 7L, "Cafe", 10, 0, 0, 0, 1, 4000D),
                        null,
                        4000D,
                        2,
                        8000D
                )),
                "Mesa 1"
        );

        when(administradorRepository.findByCorreoIgnoreCase("admin@correo.com")).thenReturn(Optional.of(admin));
        doNothing().when(administradorAccesoService).validarAccesoASede(admin, 5L);
        when(sedeRepository.findById(5L)).thenReturn(Optional.of(sede));
        when(mesaEstadoRepository.findDetalleBySedeIdAndMesaReferenciaId(5L, 2L)).thenReturn(Optional.empty());
        when(mesaEstadoRepository.saveAndFlush(any(MesaEstado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MesaEstadoDTO respuesta = mesaEstadoServicio.guardarMesa("admin@correo.com", "administrador", 5L, 2L, dto);

        assertEquals("OCUPADA", respuesta.estado());
        assertEquals(1, respuesta.carrito().size());
        assertEquals("Cafe", respuesta.carrito().get(0).producto().productoNombre());
    }

    @Test
    void listarPorSedeDebePermitirVendedorDeLaMismaSede() {
        Sede sede = new Sede();
        sede.setId(8L);

        Vendedor vendedor = new Vendedor();
        vendedor.setCorreo("vendedor@correo.com");
        vendedor.setSede(sede);

        when(vendedorRepository.findByCorreoIgnoreCase("vendedor@correo.com")).thenReturn(Optional.of(vendedor));
        when(mesaEstadoRepository.findDetalleBySedeId(8L)).thenReturn(List.of());

        List<MesaEstadoDTO> respuesta = mesaEstadoServicio.listarPorSede("vendedor@correo.com", "vendedor", 8L);

        assertEquals(0, respuesta.size());
    }

    @Test
    void guardarDomicilioDebePersistirConfiguracionYDatosDeEntrega() {
        Sede sede = new Sede();
        sede.setId(5L);
        Administrador admin = new Administrador();
        admin.setCorreo("admin@correo.com");

        MesaEstadoDTO dto = new MesaEstadoDTO(
                9991L, 1, "LIBRE", List.of(), "Domicilio norte",
                "DOMICILIO", true, 14, "Calle 10 # 20-30", 5000D,
                "Ana", "3001234567", null
        );

        when(administradorRepository.findByCorreoIgnoreCase("admin@correo.com")).thenReturn(Optional.of(admin));
        doNothing().when(administradorAccesoService).validarAccesoASede(admin, 5L);
        when(sedeRepository.findById(5L)).thenReturn(Optional.of(sede));
        when(mesaEstadoRepository.findDetalleBySedeIdAndMesaReferenciaId(5L, 9991L)).thenReturn(Optional.empty());
        when(mesaEstadoRepository.saveAndFlush(any(MesaEstado.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MesaEstadoDTO respuesta = mesaEstadoServicio.guardarMesa("admin@correo.com", "administrador", 5L, 9991L, dto);

        assertEquals("DOMICILIO", respuesta.tipo());
        assertEquals("Calle 10 # 20-30", respuesta.domicilioDireccion());
        assertEquals(5000D, respuesta.domicilioCosto());
        assertEquals("LIBRE", respuesta.estado());
    }

    @Test
    void guardarMesaDebeRechazarUnaVersionDesactualizada() {
        Sede sede = new Sede();
        sede.setId(5L);
        Administrador admin = new Administrador();
        admin.setCorreo("admin@correo.com");
        MesaEstado existente = new MesaEstado();
        existente.setId(10L);
        existente.setVersion(4L);

        MesaEstadoDTO dto = new MesaEstadoDTO(
                2L, 1, "LIBRE", List.of(), "Mesa 1",
                "MESA", true, 2, null, null, null, null, 3L
        );

        when(administradorRepository.findByCorreoIgnoreCase("admin@correo.com")).thenReturn(Optional.of(admin));
        doNothing().when(administradorAccesoService).validarAccesoASede(admin, 5L);
        when(sedeRepository.findById(5L)).thenReturn(Optional.of(sede));
        when(mesaEstadoRepository.findDetalleBySedeIdAndMesaReferenciaId(5L, 2L)).thenReturn(Optional.of(existente));

        assertThrows(MesaVersionConflictException.class,
                () -> mesaEstadoServicio.guardarMesa("admin@correo.com", "administrador", 5L, 2L, dto));
    }

    @Test
    void liberarMesaPorVentaDebeLimpiarCarritoYDomicilio() {
        MesaEstado mesa = new MesaEstado();
        mesa.setId(20L);
        mesa.setMesaReferenciaId(9991L);
        mesa.setNumero(1);
        mesa.setNombre("Domicilio 1");
        mesa.setTipo("DOMICILIO");
        mesa.setVisible(true);
        mesa.setOrdenVisual(14);
        mesa.setEstado("OCUPADA");
        mesa.setVersion(2L);
        mesa.setDomicilioDireccion("Calle 1");
        mesa.setDomicilioCosto(5000D);
        MesaEstadoItem item = new MesaEstadoItem();
        item.setMesaEstado(mesa);
        mesa.getItems().add(item);

        when(mesaEstadoRepository.findDetalleBySedeIdAndMesaReferenciaId(5L, 9991L))
                .thenReturn(Optional.of(mesa));
        when(mesaEstadoRepository.saveAndFlush(mesa)).thenReturn(mesa);

        mesaEstadoServicio.liberarMesaPorVenta(5L, 9991L);

        assertEquals("LIBRE", mesa.getEstado());
        assertEquals(0, mesa.getItems().size());
        assertEquals(null, mesa.getDomicilioDireccion());
        assertEquals(null, mesa.getDomicilioCosto());
    }
}
