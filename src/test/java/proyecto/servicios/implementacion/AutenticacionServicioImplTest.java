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
import proyecto.entidades.Sede;
import proyecto.entidades.SuscripcionSede;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.CuentaRepo;
import proyecto.repositorios.SuscripcionSedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.utils.JWTUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock
    private SuscripcionSedeRepository suscripcionSedeRepository;

    @Mock
    private VendedorRepository vendedorRepository;

    @Mock
    private SuscripcionFeatureService suscripcionFeatureService;

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
                "Empresa Uno",
                900123456L,
                3001234567L
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
                "Empresa Uno",
                900123456L,
                3001234567L
        );

        when(cuentaRepo.findLoginByCorreo("vendedor@correo.com")).thenReturn(Optional.of(vendedor));
        when(vendedorRepository.findByCorreo("vendedor@correo.com")).thenReturn(Optional.empty());
        when(jwtUtils.generarToken(eq("vendedor@correo.com"), anyMap())).thenReturn("token-falso");

        TokenDTO respuesta = autenticacionServicio.login(new LoginDTO("vendedor@correo.com", "secreta"));

        assertEquals("token-falso", respuesta.getToken());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(jwtUtils).generarToken(eq("vendedor@correo.com"), captor.capture());
        assertEquals("Empresa Uno", captor.getValue().get("nombreEmpresa"));
        assertEquals(900123456L, captor.getValue().get("companyNit"));
        assertEquals(3001234567L, captor.getValue().get("companyPhone"));
    }

    @Test
    void debeGenerarTokenDeAdministradorCuandoLaCuentaEsAdministrativa() throws Exception {
        LoginCuentaDTO administrador = crearCuentaLogin(
                20,
                "admin@correo.com",
                encoder.encode("secreta"),
                "administrador",
                "Admin",
                1,
                "Empresa Admin",
                900999111L,
                3015550000L,
                false,
                true
        );

        when(cuentaRepo.findLoginByCorreo("admin@correo.com")).thenReturn(Optional.of(administrador));
        when(jwtUtils.generarToken(eq("admin@correo.com"), anyMap())).thenReturn("token-admin");

        TokenDTO respuesta = autenticacionServicio.login(new LoginDTO("admin@correo.com", "secreta"));

        assertEquals("token-admin", respuesta.getToken());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(jwtUtils).generarToken(eq("admin@correo.com"), captor.capture());
        assertEquals("administrador", captor.getValue().get("rol"));
        assertEquals(Boolean.TRUE, captor.getValue().get("esAdministradorEmpresa"));
    }

    @Test
    void debePermitirLoginSiLaSuscripcionEstaVencidaYRetornarAdvertencia() throws Exception {
        LoginCuentaDTO vendedor = crearCuentaLogin(
                10,
                "vendedor@correo.com",
                encoder.encode("secreta"),
                "vendedor",
                "Laura",
                1,
                "Empresa Uno",
                900123456L,
                3001234567L
        );
        SuscripcionSede suscripcion = new SuscripcionSede();
        suscripcion.setActiva(true);
        suscripcion.setFechaInicioServicio(LocalDate.of(2026, 1, 1));
        suscripcion.setFechaUltimoPago(LocalDate.of(2026, 6, 1));
        suscripcion.setFechaProximoVencimiento(LocalDate.now().minusDays(1));

        Sede sede = new Sede();
        sede.setId(10L);
        Vendedor entidadVendedor = new Vendedor();
        entidadVendedor.setSede(sede);

        when(cuentaRepo.findLoginByCorreo("vendedor@correo.com")).thenReturn(Optional.of(vendedor));
        when(vendedorRepository.findByCorreo("vendedor@correo.com")).thenReturn(Optional.of(entidadVendedor));
        when(suscripcionSedeRepository.findBySedeId(10L)).thenReturn(Optional.of(suscripcion));
        when(jwtUtils.generarToken(eq("vendedor@correo.com"), anyMap())).thenReturn("token-falso");

        TokenDTO respuesta = autenticacionServicio.login(new LoginDTO("vendedor@correo.com", "secreta"));

        assertEquals("token-falso", respuesta.getToken());
        assertEquals("VENCIDO", respuesta.getEstadoSuscripcion());
        assertEquals(LocalDate.now().minusDays(1).toString(), respuesta.getFechaVencimientoSuscripcion());
        assertNotNull(respuesta.getMensajeSuscripcion());
        assertTrue(respuesta.getMensajeSuscripcion().contains("Tu suscripcion esta vencida desde el "));
        assertTrue(respuesta.getMensajeSuscripcion().contains("3026367474"));
    }

    @Test
    void debeBloquearLoginSiLaSedeEstaPendienteDeActivacionPorSoporte() {
        LoginCuentaDTO vendedor = crearCuentaLogin(
                20,
                "vendedor@correo.com",
                encoder.encode("secreta"),
                "vendedor",
                "Vendedor",
                1,
                "Empresa Nueva",
                900999111L,
                3015550000L,
                false,
                false
        );

        SuscripcionSede suscripcion = new SuscripcionSede();
        suscripcion.setActiva(true);
        suscripcion.setEstadoServicio(proyecto.entidades.EstadoSuscripcionSede.VENCIDO);
        suscripcion.setFechaProximoVencimiento(LocalDate.now().minusDays(1));
        suscripcion.setObservacion(SuscripcionSedeInicializacionService.OBSERVACION_PENDIENTE_ACTIVACION);

        Sede sede = new Sede();
        sede.setId(30L);
        Vendedor entidadVendedor = new Vendedor();
        entidadVendedor.setSede(sede);

        when(cuentaRepo.findLoginByCorreo("vendedor@correo.com")).thenReturn(Optional.of(vendedor));
        when(vendedorRepository.findByCorreo("vendedor@correo.com")).thenReturn(Optional.of(entidadVendedor));
        when(suscripcionSedeRepository.findBySedeId(30L)).thenReturn(Optional.of(suscripcion));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> autenticacionServicio.login(new LoginDTO("vendedor@correo.com", "secreta")));

        assertEquals(
                "La sede esta pendiente de activacion. Comunicate con soporte tecnico para habilitarla.",
                exception.getMessage()
        );
        verify(jwtUtils, never()).generarToken(eq("vendedor@correo.com"), anyMap());
    }

    private LoginCuentaDTO crearCuentaLogin(
            Integer codigo,
            String correo,
            String password,
            String rol,
            String nombre,
            Integer estado,
            String nombreEmpresa,
            Long empresaNit,
            Long empresaTelefono
    ) {
        return crearCuentaLogin(
                codigo,
                correo,
                password,
                rol,
                nombre,
                estado,
                nombreEmpresa,
                empresaNit,
                empresaTelefono,
                false,
                false
        );
    }

    private LoginCuentaDTO crearCuentaLogin(
            Integer codigo,
            String correo,
            String password,
            String rol,
            String nombre,
            Integer estado,
            String nombreEmpresa,
            Long empresaNit,
            Long empresaTelefono,
            Boolean esSuperAdmin,
            Boolean esAdministradorEmpresa
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

            @Override
            public Long getEmpresaNit() {
                return empresaNit;
            }

            @Override
            public Long getEmpresaTelefono() {
                return empresaTelefono;
            }

            @Override
            public Boolean getEsSuperAdmin() {
                return esSuperAdmin;
            }

            @Override
            public Boolean getEsAdministradorEmpresa() {
                return esAdministradorEmpresa;
            }
        };
    }
}
