package proyecto.servicios.implementacion;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.utils.JWTUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfiguracionCocinaSedeServiceTest {
    @Mock SedeRepository sedeRepository;
    @Mock VendedorRepository vendedorRepository;
    @Mock AdministradorAccesoService accesoService;
    @Mock JWTUtils jwtUtils;
    @Mock Jws<Claims> jws;
    @Mock Claims claims;
    @InjectMocks ConfiguracionCocinaSedeService service;

    @Test
    void actualizarSoloModificaSedeSeleccionada() {
        Empresa empresa = new Empresa();
        Sede primera = new Sede();
        primera.setId(1L);
        primera.setEmpresa(empresa);
        Sede segunda = new Sede();
        segunda.setEmpresa(empresa);
        Administrador admin = new Administrador();
        when(accesoService.obtenerAdministradorAutenticado("token")).thenReturn(admin);
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(primera));
        assertFalse(service.actualizar("token", 1L, false).habilitada());
        assertTrue(segunda.getImpresionCocinaHabilitada());
        assertTrue(empresa.getImpresionCocinaHabilitada());
        verify(accesoService).validarAccesoASede(admin, 1L);
        verify(sedeRepository).save(primera);
    }

    @Test
    void administradorSinAccesoNoModificaSede() {
        Administrador admin = new Administrador();
        when(accesoService.obtenerAdministradorAutenticado("token")).thenReturn(admin);
        doThrow(new RuntimeException("Sin acceso")).when(accesoService).validarAccesoASede(admin, 2L);
        assertThrows(RuntimeException.class, () -> service.actualizar("token", 2L, false));
        verifyNoInteractions(sedeRepository);
    }

    @Test
    void vendedorNoPuedeActualizar() {
        when(accesoService.obtenerAdministradorAutenticado("token")).thenThrow(new RuntimeException("Sin permiso"));
        assertThrows(RuntimeException.class, () -> service.actualizar("token", 1L, false));
        verifyNoInteractions(sedeRepository);
    }

    @Test
    void rechazaEstadoNulo() {
        assertThrows(IllegalArgumentException.class, () -> service.actualizar("token", 1L, null));
        verifyNoInteractions(sedeRepository);
    }

    private void token(String rol) {
        when(jwtUtils.parseJwt("token")).thenReturn(jws);
        when(jws.getBody()).thenReturn(claims);
        when(claims.get("rol", String.class)).thenReturn(rol);
    }

    @Test
    void produccionConsultaSuSede() {
        token("produccion");
        when(claims.getSubject()).thenReturn("cocina");
        Sede sede = new Sede();
        sede.setId(1L);
        sede.setImpresionCocinaHabilitada(false);
        Vendedor usuario = new Vendedor();
        usuario.setSede(sede);
        when(vendedorRepository.findByCorreoIgnoreCase("cocina")).thenReturn(Optional.of(usuario));
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(sede));
        assertFalse(service.obtener("Bearer token", 1L).habilitada());
    }

    @Test
    void produccionNoConsultaOtraSede() {
        token("produccion");
        when(claims.getSubject()).thenReturn("cocina");
        Sede sede = new Sede();
        sede.setId(1L);
        Vendedor usuario = new Vendedor();
        usuario.setSede(sede);
        when(vendedorRepository.findByCorreoIgnoreCase("cocina")).thenReturn(Optional.of(usuario));
        assertThrows(IllegalArgumentException.class, () -> service.obtener("Bearer token", 2L));
        verifyNoInteractions(sedeRepository);
    }

    @Test
    void vendedorSinAccesoNoConsultaOtraSede() {
        token("vendedor");
        doThrow(new RuntimeException("Sin acceso")).when(accesoService).validarAccesoAutenticadoASede("Bearer token", 2L);
        assertThrows(RuntimeException.class, () -> service.obtener("Bearer token", 2L));
        verifyNoInteractions(sedeRepository);
    }
}
