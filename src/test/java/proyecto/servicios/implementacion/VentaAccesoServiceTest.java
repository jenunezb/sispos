package proyecto.servicios.implementacion;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.utils.JWTUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class VentaAccesoServiceTest {
    private final ClienteServicio clientes = mock(ClienteServicio.class);
    private final AdministradorAccesoService admins = mock(AdministradorAccesoService.class);
    private final SedeRepository sedes = mock(SedeRepository.class);
    private final VentaRepository ventas = mock(VentaRepository.class);
    private final VendedorRepository vendedores = mock(VendedorRepository.class);
    private final JWTUtils jwt = mock(JWTUtils.class);
    private final VentaAccesoService acceso = new VentaAccesoService(clientes, admins, sedes, ventas, vendedores, jwt);

    @Test
    void ventaAjenaNoSePuedeConsultarCambiandoId() {
        Empresa propia = new Empresa();
        propia.setNit(123L);
        Empresa ajena = new Empresa();
        ajena.setNit(999L);
        Sede sede = new Sede();
        sede.setId(9L);
        sede.setEmpresa(ajena);
        Venta venta = new Venta();
        venta.setSede(sede);
        when(ventas.findById(7L)).thenReturn(Optional.of(venta));
        when(clientes.empresaAutenticada("Bearer token")).thenReturn(propia);
        when(sedes.findById(9L)).thenReturn(Optional.of(sede));
        assertThrows(IllegalArgumentException.class, () -> acceso.validarVenta("Bearer token", 7L));
        verifyNoInteractions(admins);
    }

    @Test
    void administradorLimitadoConservaValidacionDeSede() {
        preparar("administrador");
        doThrow(new RuntimeException("Sede no asignada")).when(admins)
                .validarAccesoAutenticadoASede("Bearer token", 1L);
        assertThrows(RuntimeException.class, () -> acceso.validarSede("Bearer token", 1L));
        verify(admins).validarAccesoAutenticadoASede("Bearer token", 1L);
    }

    @Test
    void vendedorNoPuedeSeleccionarOtraSedeDeSuEmpresa() {
        preparar("vendedor");
        Vendedor vendedor = new Vendedor();
        Sede asignada = new Sede();
        asignada.setId(2L);
        vendedor.setSede(asignada);
        when(vendedores.findByCorreo("usuario@ejemplo.com")).thenReturn(Optional.of(vendedor));
        assertThrows(IllegalArgumentException.class, () -> acceso.validarSede("Bearer token", 1L));
    }

    @SuppressWarnings("unchecked")
    private void preparar(String rol) {
        Empresa empresa = new Empresa();
        empresa.setNit(123L);
        Sede sede = new Sede();
        sede.setId(1L);
        sede.setEmpresa(empresa);
        when(clientes.empresaAutenticada("Bearer token")).thenReturn(empresa);
        when(sedes.findById(1L)).thenReturn(Optional.of(sede));
        Claims claims = Jwts.claims();
        claims.setSubject("usuario@ejemplo.com");
        claims.put("rol", rol);
        Jws<Claims> token = mock(Jws.class);
        when(token.getBody()).thenReturn(claims);
        when(jwt.parseJwt("token")).thenReturn(token);
    }
}
