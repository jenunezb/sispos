package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import proyecto.dto.LoginDTO;
import proyecto.dto.TokenDTO;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.CuentaRepo;
import proyecto.utils.JWTUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacionServicioImplTest {

    @Mock
    private CuentaRepo cuentaRepo;

    @Mock
    private JWTUtils jwtUtils;

    @InjectMocks
    private AutenticacionServicioImpl autenticacionServicio;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void noDebePermitirLoginSiElVendedorEstaDesactivado() {
        Vendedor vendedor = new Vendedor();
        vendedor.setCorreo("vendedor@correo.com");
        vendedor.setPassword(encoder.encode("secreta"));
        vendedor.setEstado(false);

        when(cuentaRepo.findByCorreo("vendedor@correo.com")).thenReturn(Optional.of(vendedor));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> autenticacionServicio.login(new LoginDTO("vendedor@correo.com", "secreta")));

        assertEquals("El vendedor se encuentra desactivado. Comuníquese con el administrador.", exception.getMessage());
        verify(jwtUtils, never()).generarToken(eq("vendedor@correo.com"), anyMap());
    }

    @Test
    void debePermitirLoginSiElVendedorEstaActivo() throws Exception {
        Vendedor vendedor = new Vendedor();
        vendedor.setCodigo(10);
        vendedor.setNombre("Laura");
        vendedor.setCorreo("vendedor@correo.com");
        vendedor.setPassword(encoder.encode("secreta"));
        vendedor.setEstado(true);

        when(cuentaRepo.findByCorreo("vendedor@correo.com")).thenReturn(Optional.of(vendedor));
        when(jwtUtils.generarToken(eq("vendedor@correo.com"), anyMap())).thenReturn("token-falso");

        TokenDTO respuesta = autenticacionServicio.login(new LoginDTO("vendedor@correo.com", "secreta"));

        assertEquals("token-falso", respuesta.getToken());
        verify(jwtUtils).generarToken(eq("vendedor@correo.com"), anyMap());
    }
}
