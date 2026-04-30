package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.ComandaCocinaCrearDTO;
import proyecto.dto.ComandaCocinaDetalleDTO;
import proyecto.dto.ComandaCocinaResponseDTO;
import proyecto.entidades.*;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.ComandaCocinaRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComandaCocinaServicioImplTest {

    @Mock
    private ComandaCocinaRepository comandaCocinaRepository;
    @Mock
    private SedeRepository sedeRepository;
    @Mock
    private VendedorRepository vendedorRepository;
    @Mock
    private AdministradorRepository administradorRepository;

    @InjectMocks
    private ComandaCocinaServicioImpl comandaCocinaServicio;

    @Test
    void crearComandaDebeGuardarItemsYResponsableVendedor() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);
        empresa.setImpresionCocinaHabilitada(true);

        Sede sede = new Sede();
        sede.setId(7L);
        sede.setUbicacion("Zona norte");
        sede.setEmpresa(empresa);

        Vendedor vendedor = new Vendedor();
        vendedor.setCorreo("vendedor@correo.com");
        vendedor.setNombre("Jhon Fredy");
        vendedor.setSede(sede);

        ComandaCocinaCrearDTO dto = new ComandaCocinaCrearDTO(
                "vendedor@correo.com",
                7L,
                "Mesa 4",
                "Sin cebolla",
                List.of(
                        new ComandaCocinaDetalleDTO("Hamburguesa", 2),
                        new ComandaCocinaDetalleDTO("Papas", 1)
                )
        );

        when(administradorRepository.findByCorreoIgnoreCase("vendedor@correo.com")).thenReturn(Optional.empty());
        when(vendedorRepository.findByCorreoIgnoreCase("vendedor@correo.com")).thenReturn(Optional.of(vendedor));
        when(sedeRepository.findById(7L)).thenReturn(Optional.of(sede));
        when(comandaCocinaRepository.save(any(ComandaCocina.class))).thenAnswer(invocation -> {
            ComandaCocina comanda = invocation.getArgument(0);
            comanda.setId(15L);
            return comanda;
        });

        ComandaCocinaResponseDTO response = comandaCocinaServicio.crearComanda(dto);

        assertEquals(15L, response.id());
        assertEquals(3, response.totalItems());
        assertEquals("Jhon Fredy", response.responsable());
        assertEquals(2, response.detalles().size());

        ArgumentCaptor<ComandaCocina> captor = ArgumentCaptor.forClass(ComandaCocina.class);
        verify(comandaCocinaRepository).save(captor.capture());
        assertEquals(EstadoComandaCocina.PENDIENTE, captor.getValue().getEstado());
        assertEquals(2, captor.getValue().getDetalles().size());
    }

    @Test
    void crearComandaDebeFallarSiLaEmpresaNoTieneImpresionCocinaHabilitada() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);
        empresa.setImpresionCocinaHabilitada(false);

        Sede sede = new Sede();
        sede.setId(7L);
        sede.setEmpresa(empresa);

        Vendedor vendedor = new Vendedor();
        vendedor.setCorreo("vendedor@correo.com");
        vendedor.setSede(sede);

        ComandaCocinaCrearDTO dto = new ComandaCocinaCrearDTO(
                "vendedor@correo.com",
                7L,
                "Mesa 4",
                null,
                List.of(new ComandaCocinaDetalleDTO("Hamburguesa", 1))
        );

        when(administradorRepository.findByCorreoIgnoreCase("vendedor@correo.com")).thenReturn(Optional.empty());
        when(vendedorRepository.findByCorreoIgnoreCase("vendedor@correo.com")).thenReturn(Optional.of(vendedor));

        assertThrows(RuntimeException.class, () -> comandaCocinaServicio.crearComanda(dto));
    }

    @Test
    void actualizarEstadoDebeResolverEmpresaDesdeProduccion() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(9L);
        sede.setEmpresa(empresa);

        Vendedor produccion = new Vendedor();
        produccion.setCorreo("produccion@correo.com");
        produccion.setNombre("Cocina");
        produccion.setSede(sede);

        ComandaCocina comanda = new ComandaCocina();
        comanda.setId(3L);
        comanda.setNombreMesa("Mesa 1");
        comanda.setSede(sede);
        comanda.setEstado(EstadoComandaCocina.PENDIENTE);
        comanda.setTotalItems(1);
        comanda.setDetalles(List.of());

        when(administradorRepository.findByCorreoIgnoreCase("produccion@correo.com")).thenReturn(Optional.empty());
        when(vendedorRepository.findByCorreoIgnoreCase("produccion@correo.com")).thenReturn(Optional.of(produccion));
        when(comandaCocinaRepository.findDetalleByEmpresaNitAndId(900123456L, 3L)).thenReturn(Optional.of(comanda));
        when(comandaCocinaRepository.save(any(ComandaCocina.class))).thenAnswer(inv -> inv.getArgument(0));

        ComandaCocinaResponseDTO response = comandaCocinaServicio.actualizarEstado(
                "produccion@correo.com",
                3L,
                EstadoComandaCocina.LISTA
        );

        assertEquals(EstadoComandaCocina.LISTA, response.estado());
        assertNull(response.observaciones());
    }
}
