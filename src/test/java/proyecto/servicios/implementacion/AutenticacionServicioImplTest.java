package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import proyecto.dto.LoginCuentaDTO;
import proyecto.dto.LoginDTO;
import proyecto.dto.TokenDTO;
import proyecto.repositorios.CuentaRepo;
import proyecto.utils.JWTUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.mockito.ArgumentCaptor;

import java.util.Map;

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
        LoginCuentaDTO vendedor = crearCuentaLogin(
                10,
                "vendedor@correo.com",
                encoder.encode("secreta"),
                "vendedor",
                "Laura",
                0,
                "Empresa Uno"
        );

        when(cuentaRepo.findLoginByCorreo("vendedor@correo.com")).thenReturn(Optional.of(vendedor));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> autenticacionServicio.login(new LoginDTO("vendedor@correo.com", "secreta")));

        assertEquals("El vendedor se encuentra desactivado. Comuníquese con el administrador.", exception.getMessage());
        verify(jwtUtils, never()).generarToken(eq("vendedor@correo.com"), anyMap());
    }

    @Test
    void debePermitirLoginSiElVendedorEstaActivo() throws Exception {
        LoginCuentaDTO vendedor = crearCuentaLogin(
                10,
                "vendedor@correo.com",
                encoder.encode("secreta"),
                "vendedor",
                "Laura",
                1,
                "Empresa Uno"
        );

        when(cuentaRepo.findLoginByCorreo("vendedor@correo.com")).thenReturn(Optional.of(vendedor));
        when(jwtUtils.generarToken(eq("vendedor@correo.com"), anyMap())).thenReturn("token-falso");

        TokenDTO respuesta = autenticacionServicio.login(new LoginDTO("vendedor@correo.com", "secreta"));

        assertEquals("token-falso", respuesta.getToken());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(jwtUtils).generarToken(eq("vendedor@correo.com"), captor.capture());
        assertEquals("Empresa Uno", captor.getValue().get("nombreEmpresa"));
    }

    private LoginCuentaDTO crearCuentaLogin(
            Integer codigo,
            String correo,
            String password,
            String rol,
            String nombre,
            Integer estado,
            String nombreEmpresa
    ) {
        return new LoginCuentaDTO() {
            @Override
            public Integer getCodigo() {
                return codigo;
            }

            @Override
            public String getCorreo() {
                return correo;
            }

            @Override
            public String getPassword() {
                return password;
            }

            @Override
            public String getRol() {
                return rol;
            }

            @Override
            public String getNombre() {
                return nombre;
            }

            @Override
            public Integer getEstado() {
                return estado;
            }

            @Override
            public String getNombreEmpresa() {
                return nombreEmpresa;
            }
        };
    }
}
