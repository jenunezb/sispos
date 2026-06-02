package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import proyecto.dto.CiudadGetDTO;
import proyecto.dto.LoginCuentaDTO;
import proyecto.dto.LoginDTO;
import proyecto.dto.TokenDTO;
import proyecto.entidades.EstadoSuscripcionSede;
import proyecto.entidades.SuscripcionSede;
import proyecto.repositorios.CuentaRepo;
import proyecto.repositorios.SuscripcionSedeRepository;
import proyecto.servicios.interfaces.AutenticacionServicio;
import proyecto.utils.JWTUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AutenticacionServicioImpl implements AutenticacionServicio {
    private static final long DIAS_POR_VENCER = 5;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final CuentaRepo cuentaRepo;
    private final JWTUtils jwtUtils;
    private final SuscripcionSedeRepository suscripcionSedeRepository;

    @Override
    public TokenDTO login(LoginDTO loginDTO) throws Exception {

        // Buscar credenciales de login sin hidratar relaciones pesadas (ej. ciudad del vendedor)
        LoginCuentaDTO cuenta = cuentaRepo.findLoginByCorreo(loginDTO.email())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if ("vendedor".equals(cuenta.getRol()) && (cuenta.getEstado() == null || cuenta.getEstado() != 1)) {
            throw new RuntimeException(
                    "El vendedor se encuentra desactivado. Comuníquese con el administrador."
            );
        }

        // Validar contraseña
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (!passwordEncoder.matches(loginDTO.password(), cuenta.getPassword())) {
            throw new Exception("La contraseña ingresada es incorrecta");
        }

        SuscripcionLoginInfo suscripcionInfo = evaluarSuscripcionLogin(cuenta);

        if (suscripcionInfo.bloquearLogin()) {
            throw new RuntimeException(suscripcionInfo.mensaje());
        }

        // Generar y retornar token
        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setToken(crearToken(cuenta));
        tokenDTO.setEstadoSuscripcion(suscripcionInfo.estado());
        tokenDTO.setFechaVencimientoSuscripcion(suscripcionInfo.fechaVencimiento());
        tokenDTO.setMensajeSuscripcion(suscripcionInfo.advertir() ? suscripcionInfo.mensaje() : null);
        return tokenDTO;
    }


    @Override
    public List<CiudadGetDTO> listarCiudades() {
        return List.of();
    }

    private String crearToken(LoginCuentaDTO cuenta) {
        Map<String, Object> map = new HashMap<>();
        map.put("rol", cuenta.getRol());
        map.put("nombre", cuenta.getNombre());
        map.put("id", cuenta.getCodigo());
        map.put("nombreEmpresa", cuenta.getNombreEmpresa());
        map.put("empresaNit", cuenta.getEmpresaNit());
        map.put("companyNit", cuenta.getEmpresaNit());
        map.put("empresaTelefono", cuenta.getEmpresaTelefono());
        map.put("companyPhone", cuenta.getEmpresaTelefono());
        map.put("esSuperAdmin", Boolean.TRUE.equals(cuenta.getEsSuperAdmin()));
        map.put("esAdministradorEmpresa", Boolean.TRUE.equals(cuenta.getEsAdministradorEmpresa()));

        return jwtUtils.generarToken(cuenta.getCorreo(), map);
    }

    private SuscripcionLoginInfo evaluarSuscripcionLogin(LoginCuentaDTO cuenta) {
        if (Boolean.TRUE.equals(cuenta.getEsSuperAdmin()) || cuenta.getEmpresaNit() == null) {
            return SuscripcionLoginInfo.sinNovedad();
        }

        List<SuscripcionSede> suscripciones = suscripcionSedeRepository.findBySedeEmpresaNit(cuenta.getEmpresaNit());
        if (suscripciones.isEmpty()) {
            return SuscripcionLoginInfo.sinNovedad();
        }

        List<SuscripcionEstadoInfo> estados = suscripciones.stream()
                .filter(suscripcion -> !esSuscripcionSinConfigurar(suscripcion))
                .map(this::evaluarEstado)
                .toList();

        if (estados.isEmpty()) {
            return SuscripcionLoginInfo.sinNovedad();
        }

        SuscripcionEstadoInfo vencida = estados.stream()
                .filter(item -> item.estado() == EstadoSuscripcionSede.VENCIDO || item.estado() == EstadoSuscripcionSede.SUSPENDIDO)
                .min(Comparator.comparing(SuscripcionEstadoInfo::fechaOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        if (vencida != null) {
            String fecha = formatearFechaMensaje(vencida.fechaVencimiento());
            return new SuscripcionLoginInfo(
                    true,
                    false,
                    vencida.estado().name(),
                    vencida.fechaVencimiento() != null ? DATE_FORMATTER.format(vencida.fechaVencimiento()) : null,
                    "Tu suscripcion esta vencida desde el " + fecha + ". Realiza el pago por llave al numero 3026367474 y, si ya pagaste, envia el comprobante para activar nuevamente el servicio."
            );
        }

        SuscripcionEstadoInfo porVencer = estados.stream()
                .filter(item -> item.estado() == EstadoSuscripcionSede.POR_VENCER)
                .min(Comparator.comparing(SuscripcionEstadoInfo::fechaOrden, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        if (porVencer != null) {
            return new SuscripcionLoginInfo(
                    false,
                    true,
                    porVencer.estado().name(),
                    porVencer.fechaVencimiento() != null ? DATE_FORMATTER.format(porVencer.fechaVencimiento()) : null,
                    "Tu suscripcion esta por vencer."
            );
        }

        return SuscripcionLoginInfo.sinNovedad();
    }

    private SuscripcionEstadoInfo evaluarEstado(SuscripcionSede suscripcion) {
        if (!Boolean.TRUE.equals(suscripcion.getActiva())) {
            return new SuscripcionEstadoInfo(EstadoSuscripcionSede.SUSPENDIDO, suscripcion.getFechaProximoVencimiento());
        }

        LocalDate vencimiento = suscripcion.getFechaProximoVencimiento();
        if (vencimiento == null) {
            return new SuscripcionEstadoInfo(EstadoSuscripcionSede.VENCIDO, null);
        }

        LocalDate hoy = LocalDate.now();
        if (vencimiento.isAfter(hoy.plusDays(DIAS_POR_VENCER))) {
            return new SuscripcionEstadoInfo(EstadoSuscripcionSede.ACTIVO, vencimiento);
        }

        if (!vencimiento.isBefore(hoy)) {
            return new SuscripcionEstadoInfo(EstadoSuscripcionSede.POR_VENCER, vencimiento);
        }

        return new SuscripcionEstadoInfo(EstadoSuscripcionSede.VENCIDO, vencimiento);
    }

    private boolean esSuscripcionSinConfigurar(SuscripcionSede suscripcion) {
        if (suscripcion == null) {
            return true;
        }

        return suscripcion.getFechaInicioServicio() == null
                && suscripcion.getFechaUltimoPago() == null
                && suscripcion.getFechaProximoVencimiento() == null;
    }

    private String formatearFechaMensaje(LocalDate fecha) {
        if (fecha == null) {
            return "fecha no disponible";
        }

        String mes = switch (fecha.getMonthValue()) {
            case 1 -> "enero";
            case 2 -> "febrero";
            case 3 -> "marzo";
            case 4 -> "abril";
            case 5 -> "mayo";
            case 6 -> "junio";
            case 7 -> "julio";
            case 8 -> "agosto";
            case 9 -> "septiembre";
            case 10 -> "octubre";
            case 11 -> "noviembre";
            case 12 -> "diciembre";
            default -> "";
        };

        return fecha.getDayOfMonth() + " de " + mes + " de " + fecha.getYear();
    }

    private record SuscripcionEstadoInfo(EstadoSuscripcionSede estado, LocalDate fechaVencimiento) {
        private LocalDate fechaOrden() {
            return fechaVencimiento;
        }
    }

    private record SuscripcionLoginInfo(
            boolean bloquearLogin,
            boolean advertir,
            String estado,
            String fechaVencimiento,
            String mensaje
    ) {
        private static SuscripcionLoginInfo sinNovedad() {
            return new SuscripcionLoginInfo(false, false, null, null, null);
        }
    }
}
