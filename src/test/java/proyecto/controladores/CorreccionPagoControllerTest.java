package proyecto.controladores;

import io.jsonwebtoken.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import proyecto.excepciones.GlobalExceptionHandler;
import proyecto.servicios.implementacion.CorreccionPagoService;
import proyecto.utils.FiltroToken;
import proyecto.utils.JWTUtils;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CorreccionPagoControllerTest {
    CorreccionPagoService service = mock(CorreccionPagoService.class);
    JWTUtils jwt = mock(JWTUtils.class);
    Claims claims;
    MockMvc mvc;
    String body = "{\"modoPagoAnterior\":\"EFECTIVO\",\"modoPago\":\"TRANSFERENCIA\",\"motivo\":\"Error de registro\"}";

    @BeforeEach @SuppressWarnings("unchecked")
    void preparar() {
        claims = Jwts.claims(); claims.setSubject("qa@test.com"); claims.put("rol", "vendedor");
        Jws<Claims> token = mock(Jws.class); when(token.getBody()).thenReturn(claims);
        when(jwt.parseJwt("token")).thenReturn(token);
        mvc = MockMvcBuilders.standaloneSetup(new CorreccionPagoController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).addFilters(new FiltroToken(jwt)).build();
    }

    @Test void vendedorYAdminLleganAlServicioConIdentidadAutenticada() throws Exception {
        for (String rol : new String[]{"vendedor", "administrador"}) {
            claims.put("rol", rol);
            mvc.perform(patch("/api/ventas/1/pago").header("Authorization", "Bearer token")
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        }
        verify(service, times(2)).corregir(eq("Bearer token"), eq(1L), any());
    }

    @Test void sinTokenOCocinaNoAcceden() throws Exception {
        mvc.perform(patch("/api/ventas/1/pago").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        claims.put("rol", "cocina");
        mvc.perform(patch("/api/ventas/1/pago").header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void rechazaSolicitudSinMotivoOMedio() throws Exception {
        for (String invalid : new String[]{"{}", "{\"modoPagoAnterior\":\"EFECTIVO\",\"modoPago\":\"TRANSFERENCIA\",\"motivo\":\"   \"}"}) {
            mvc.perform(patch("/api/ventas/1/pago").header("Authorization", "Bearer token")
                    .contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test void historialUsaValidacionDelServicio() throws Exception {
        mvc.perform(get("/api/ventas/1/pago/historial").header("Authorization", "Bearer token"))
                .andExpect(status().isOk());
        verify(service).historial("Bearer token", 1L);
    }
}
