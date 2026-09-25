package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import proyecto.entidades.*;
import proyecto.excepciones.GlobalExceptionHandler;
import proyecto.repositorios.*;
import proyecto.servicios.implementacion.ClienteServicio;
import proyecto.utils.FiltroToken;
import proyecto.utils.JWTUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClienteControllerTest {
    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final AdministradorRepository administradores = mock(AdministradorRepository.class);
    private final VendedorRepository vendedores = mock(VendedorRepository.class);
    private final JWTUtils jwt = mock(JWTUtils.class);
    private MockMvc mvc;
    private Empresa empresa;

    @BeforeEach
    void preparar() {
        empresa = new Empresa();
        empresa.setNit(123L);
        var servicio = new ClienteServicio(clientes, administradores, vendedores, jwt);
        mvc = MockMvcBuilders.standaloneSetup(new ClienteController(servicio))
                .setControllerAdvice(new GlobalExceptionHandler()).addFilters(new FiltroToken(jwt)).build();
    }

    @SuppressWarnings("unchecked")
    private void autenticar(String rol) {
        Claims claims = Jwts.claims();
        claims.setSubject("usuario@empresa.com");
        claims.put("rol", rol);
        Jws<Claims> token = mock(Jws.class);
        when(token.getBody()).thenReturn(claims);
        when(jwt.parseJwt("token")).thenReturn(token);
        Administrador admin = new Administrador();
        admin.setEmpresa(empresa);
        admin.setEsAdministradorEmpresa(false); // Administrador limitado.
        when(administradores.findByCorreo("usuario@empresa.com")).thenReturn(Optional.of(admin));
        Vendedor vendedor = new Vendedor();
        vendedor.setEmpresa(empresa);
        vendedor.setTipoPerfil("produccion".equals(rol) ? TipoPerfilVendedor.PRODUCCION : TipoPerfilVendedor.VENDEDOR);
        when(vendedores.findByCorreo("usuario@empresa.com")).thenReturn(Optional.of(vendedor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"administrador", "vendedor", "produccion"})
    void listaDirectorioDeEmpresaDelTokenSinAceptarEmpresaSolicitada(String rol) throws Exception {
        autenticar(rol);
        Cliente cliente = new Cliente();
        cliente.setId(7L);
        cliente.setNombre("Cliente compartido");
        when(clientes.findByEmpresaNitAndActivoTrueOrderByNombreAsc(123L)).thenReturn(List.of(cliente));
        mvc.perform(get("/api/clientes?empresaNit=999&correo=otro@empresa.com")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.error").value(false))
                .andExpect(jsonPath("$.respuesta[0].id").value(7));
        verify(clientes).findByEmpresaNitAndActivoTrueOrderByNombreAsc(123L);
        verifyNoMoreInteractions(clientes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"administrador", "vendedor", "produccion"})
    void noPermiteEditarIdDeOtraEmpresa(String rol) throws Exception {
        autenticar(rol);
        when(clientes.findByIdAndEmpresaNit(99L, 123L)).thenReturn(Optional.empty());
        mvc.perform(put("/api/clientes/99").header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ajeno\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(true));
        verify(clientes, never()).save(any());
    }

    @Test
    void creaClienteCompletoConIdYEmpresaAutenticada() throws Exception {
        autenticar("vendedor");
        when(clientes.save(any())).thenAnswer(inv -> {
            Cliente cliente = inv.getArgument(0);
            assertSame(empresa, cliente.getEmpresa());
            cliente.setId(8L);
            return cliente;
        });
        mvc.perform(post("/api/clientes").header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Empresa A","nit":"900-1","correo":"cliente@ejemplo.com",
                         "telefono":"3001234567","direccion":"Calle 1","empresaNit":999}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.respuesta.id").value(8))
                .andExpect(jsonPath("$.respuesta.documento").value("900-1"))
                .andExpect(jsonPath("$.respuesta.correo").value("cliente@ejemplo.com"))
                .andExpect(jsonPath("$.respuesta.telefono").value("3001234567"))
                .andExpect(jsonPath("$.respuesta.direccion").value("Calle 1"));
    }

    @Test
    void actualizaClientePropioConTodosLosCampos() throws Exception {
        autenticar("administrador");
        Cliente cliente = new Cliente();
        cliente.setId(8L);
        cliente.setEmpresa(empresa);
        when(clientes.findByIdAndEmpresaNit(8L, 123L)).thenReturn(Optional.of(cliente));
        when(clientes.save(cliente)).thenReturn(cliente);
        mvc.perform(put("/api/clientes/8").header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Nuevo nombre","documento":"CC123","correo":"nuevo@ejemplo.com",
                         "telefono":"111","direccion":"Nueva direccion"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.respuesta.id").value(8))
                .andExpect(jsonPath("$.respuesta.nombre").value("Nuevo nombre"))
                .andExpect(jsonPath("$.respuesta.correo").value("nuevo@ejemplo.com"))
                .andExpect(jsonPath("$.respuesta.direccion").value("Nueva direccion"));
        assertSame(empresa, cliente.getEmpresa());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"nombre\":\"   \"}", "{\"nombre\":\"A\",\"correo\":\"invalido\"}"})
    void validaNombreYCorreoEnCrearYEditar(String body) throws Exception {
        autenticar("administrador");
        mvc.perform(post("/api/clientes").header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(true));
        mvc.perform(put("/api/clientes/8").header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(true));
        verifyNoInteractions(clientes);
    }

    @Test
    void permiteSoloNombre() throws Exception {
        autenticar("produccion");
        when(clientes.save(any())).thenAnswer(inv -> {
            Cliente cliente = inv.getArgument(0);
            cliente.setId(8L);
            return cliente;
        });
        mvc.perform(post("/api/clientes").header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"A\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.respuesta.id").value(8));
    }

    @Test
    void rechazaSinTokenYRolNoAutorizado() throws Exception {
        mvc.perform(get("/api/clientes")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(true));
        autenticar("desconocido");
        mvc.perform(get("/api/clientes").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(clientes);
    }
}
