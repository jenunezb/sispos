package proyecto.servicios.implementacion;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import proyecto.dto.MeseroDTO;
import proyecto.entidades.Mesero;
import proyecto.entidades.Sede;
import proyecto.repositorios.MeseroRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.utils.JWTUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MeseroCuentaService {
    private final MeseroRepository meseroRepository;
    private final SedeRepository sedeRepository;
    private final AdministradorAccesoService administradorAccesoService;
    private final JWTUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String hashSinCuenta = passwordEncoder.encode(UUID.randomUUID().toString());

    @Transactional(readOnly = true)
    public List<MeseroDTO.Cuenta> listar(String authorization, Long sedeId) {
        validarAdministrador(authorization, sedeId);
        return meseroRepository.findBySedeIdOrderByNombreAsc(sedeId).stream().map(this::cuenta).toList();
    }

    @Transactional
    public MeseroDTO.Cuenta crear(String authorization, Long sedeId, MeseroDTO.Alta dto) {
        validarAdministrador(authorization, sedeId);
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sede no encontrada"));
        Mesero mesero = new Mesero();
        mesero.setSede(sede);
        mesero.setNombre(dto.nombre().trim());
        mesero.setUsuario(normalizar(dto.usuario()));
        if (meseroRepository.existsByUsuario(mesero.getUsuario())) {
            throw usuarioOcupado();
        }

        mesero.setPasswordHash(codificarPassword(dto.password()));
        mesero.setClaveSesion(UUID.randomUUID().toString());
        try {
            return cuenta(meseroRepository.saveAndFlush(mesero));
        } catch (DataIntegrityViolationException ex) {
            throw usuarioOcupado();
        }
    }

    @Transactional
    public MeseroDTO.Cuenta actualizar(String authorization, Long sedeId, Long meseroId, MeseroDTO.Edicion dto) {
        validarAdministrador(authorization, sedeId);
        Mesero mesero = meseroRepository.findByIdAndSedeId(meseroId, sedeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mesero no encontrado"));
        mesero.setNombre(dto.nombre().trim());
        if (dto.password() != null || mesero.isActivo() != dto.activo()) {
            mesero.setClaveSesion(UUID.randomUUID().toString());
        }
        if (dto.password() != null) {
            mesero.setPasswordHash(codificarPassword(dto.password()));
        }
        mesero.setActivo(dto.activo());
        return cuenta(mesero);
    }

    @Transactional(readOnly = true)
    public MeseroDTO.Sesion login(MeseroDTO.Login dto) {
        Mesero mesero = meseroRepository.findByUsuario(normalizar(dto.usuario())).orElse(null);
        String hash = mesero == null ? hashSinCuenta : mesero.getPasswordHash();
        boolean valida = dto.password().getBytes(StandardCharsets.UTF_8).length <= 72
                && passwordEncoder.matches(dto.password(), hash);
        if (!valida || mesero == null || !mesero.isActivo()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }
        return new MeseroDTO.Sesion(jwtUtils.generarTokenMesero(mesero.getId(), mesero.getClaveSesion()),
                mesero.getNombre(), mesero.getSede().getUbicacion(),
                Boolean.TRUE.equals(mesero.getSede().getImpresionCocinaHabilitada()));
    }

    public Mesero obtenerMesero(Claims claims) {
        return meseroRepository.findByIdAndActivoTrue(Long.valueOf(claims.getSubject()))
                .filter(mesero -> mesero.getClaveSesion().equals(claims.get("claveSesion", String.class)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vuelve a iniciar sesión"));
    }

    private void validarAdministrador(String authorization, Long sedeId) {
        var admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        if (!administradorAccesoService.tieneAccesoASede(admin, sedeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta sede");
        }
    }

    private String codificarPassword(String password) {
        if (password.isBlank() || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe contener entre 8 y 72 bytes");
        }
        return passwordEncoder.encode(password);
    }

    private String normalizar(String usuario) {
        return usuario.trim().toLowerCase(Locale.ROOT);
    }

    private MeseroDTO.Cuenta cuenta(Mesero mesero) {
        return new MeseroDTO.Cuenta(mesero.getId(), mesero.getUsuario(), mesero.getNombre(), mesero.isActivo());
    }

    private ResponseStatusException usuarioOcupado() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "El nombre de usuario ya está en uso");
    }
}
