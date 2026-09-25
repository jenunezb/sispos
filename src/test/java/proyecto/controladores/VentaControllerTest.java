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
import proyecto.dto.VentaRecuestDTO;
import proyecto.dto.VentaResponseDTO;
import java.util.List;
import proyecto.entidades.Venta;
import proyecto.excepciones.GlobalExceptionHandler;
import proyecto.servicios.implementacion.VentaAccesoService;
import proyecto.servicios.interfaces.ComandaCocinaServicio;
import proyecto.servicios.interfaces.VentaServicio;
import proyecto.utils.FiltroToken;
import proyecto.utils.JWTUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VentaControllerTest {
    private final VentaServicio ventas = mock(VentaServicio.class);
    private final VentaAccesoService acceso = mock(VentaAccesoService.class);
    private final JWTUtils jwt = mock(JWTUtils.class);
    private MockMvc mvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        Claims claims = Jwts.claims();
        claims.setSubject("autenticado@ejemplo.com");
        claims.put("rol", "vendedor");
        Jws<Claims> token = mock(Jws.class);
        when(token.getBody()).thenReturn(claims);
        when(jwt.parseJwt("token")).thenReturn(token);
        mvc = MockMvcBuilders.standaloneSetup(new VentaController(ventas,
                        mock(ComandaCocinaServicio.class), acceso, jwt))
                .setControllerAdvice(new GlobalExceptionHandler()).addFilters(new FiltroToken(jwt)).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "\"\"", "\"   \""})
    void aceptaClienteVacioYUsaIdentidadDelToken(String clienteId) throws Exception {
        when(ventas.crearVentaAutenticada(any(), eq("vendedor"))).thenAnswer(inv -> {
            VentaRecuestDTO dto = inv.getArgument(0);
            assertNull(dto.clienteId());
            assertEquals("autenticado@ejemplo.com", dto.correo());
            return new Venta();
        });
        mvc.perform(post("/api/ventas").header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"correo":"ajeno@ejemplo.com","sedeId":1,"clienteId":%s,"detalles":[]}
                        """.formatted(clienteId)))
                .andExpect(status().isOk());
        verify(acceso).validarSede("Bearer token", 1L);
        verify(ventas).crearVentaAutenticada(any(), eq("vendedor"));
    }

    @Test
    void noCreaVentaEnSedeNoAutorizada() throws Exception {
        doThrow(new IllegalArgumentException("Sede no autorizada")).when(acceso).validarSede("Bearer token", 99L);
        mvc.perform(post("/api/ventas").header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sedeId\":99,\"clienteId\":7}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(true));
        verifyNoInteractions(ventas);
    }

    @Test
    void noDevuelveVentaAjena() throws Exception {
        doThrow(new IllegalArgumentException("Venta no autorizada")).when(acceso).validarVenta("Bearer token", 99L);
        mvc.perform(get("/api/ventas/99").header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(true));
        verifyNoInteractions(ventas);
    }

    @Test
    void mantieneContratoDeVentaYListadoDelFrontendActual() throws Exception {
        VentaResponseDTO venta = new VentaResponseDTO(7L, 1L, null, 1000.0, "EFECTIVO",
                1000.0, 0.0, "Vendedor", "Centro", null, null, null, false, true, List.of());
        when(ventas.obtenerVentaPorId(7L)).thenReturn(venta);
        when(ventas.listarVentasPorSede(1L)).thenReturn(List.of(venta));
        mvc.perform(get("/api/ventas/7").header("Authorization", "Bearer token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.respuesta").doesNotExist());
        mvc.perform(get("/api/ventas/sede/1").header("Authorization", "Bearer token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(7));
        when(ventas.crearVentaAutenticada(any(), eq("vendedor"))).thenReturn(new Venta());
        when(ventas.mapToResponse(any())).thenReturn(venta);
        mvc.perform(post("/api/ventas").header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sedeId\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.respuesta").doesNotExist());
    }
}
