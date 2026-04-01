package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.InventarioDTO;
import proyecto.dto.MesaEstadoDTO;
import proyecto.dto.MesaEstadoItemDTO;
import proyecto.entidades.Administrador;
import proyecto.entidades.MesaEstado;
import proyecto.entidades.Sede;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.MesaEstadoRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        when(mesaEstadoRepository.save(any(MesaEstado.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
}
