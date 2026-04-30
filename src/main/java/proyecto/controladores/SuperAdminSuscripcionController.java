package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.entidades.Administrador;
import proyecto.repositorios.AdministradorRepository;
import proyecto.servicios.interfaces.SuperAdminSuscripcionServicio;
import proyecto.utils.JWTUtils;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/superadmin/suscripciones")
@RequiredArgsConstructor
public class SuperAdminSuscripcionController {

    private final SuperAdminSuscripcionServicio superAdminSuscripcionServicio;
    private final AdministradorRepository administradorRepository;
    private final JWTUtils jwtUtils;

    @GetMapping("/sedes")
    public ResponseEntity<MensajeDTO<List<SuperAdminSuscripcionSedeDTO>>> listarSuscripciones(
            @RequestHeader("Authorization") String authorization
    ) {
        validarSuperAdmin(authorization);
        return ResponseEntity.ok(new MensajeDTO<>(false, superAdminSuscripcionServicio.listarSuscripciones()));
    }

    @GetMapping("/sedes/{sedeId}")
    public ResponseEntity<MensajeDTO<SuperAdminSuscripcionSedeDTO>> obtenerSuscripcionPorSede(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long sedeId
    ) {
        validarSuperAdmin(authorization);
        return ResponseEntity.ok(new MensajeDTO<>(false, superAdminSuscripcionServicio.obtenerSuscripcionPorSede(sedeId)));
    }

    @PostMapping("/sedes/configurar")
    public ResponseEntity<MensajeDTO<SuperAdminSuscripcionSedeDTO>> configurarSuscripcion(
            @RequestHeader("Authorization") String authorization,
            @RequestBody SuperAdminConfigurarSuscripcionDTO dto
    ) {
        validarSuperAdmin(authorization);
        return ResponseEntity.ok(new MensajeDTO<>(false, superAdminSuscripcionServicio.configurarSuscripcion(dto)));
    }

    @PostMapping("/pagos")
    public ResponseEntity<MensajeDTO<SuperAdminPagoSuscripcionDTO>> registrarPago(
            @RequestHeader("Authorization") String authorization,
            @RequestBody SuperAdminRegistrarPagoSuscripcionDTO dto
    ) {
        String correo = validarSuperAdmin(authorization);
        return ResponseEntity.ok(new MensajeDTO<>(false, superAdminSuscripcionServicio.registrarPago(dto, correo)));
    }

    @GetMapping("/pagos")
    public ResponseEntity<MensajeDTO<List<SuperAdminPagoSuscripcionDTO>>> listarPagos(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long sedeId
    ) {
        validarSuperAdmin(authorization);
        return ResponseEntity.ok(new MensajeDTO<>(false, superAdminSuscripcionServicio.listarPagos(sedeId)));
    }

    private String validarSuperAdmin(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        String correo = claims.getBody().getSubject();
        String rol = (String) claims.getBody().get("rol");
        Boolean esSuperAdmin = claims.getBody().get("esSuperAdmin", Boolean.class);

        if (!"administrador".equals(rol) || !Boolean.TRUE.equals(esSuperAdmin)) {
            throw new RuntimeException("No tiene permisos de superadministrador");
        }

        Administrador administrador = administradorRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));

        if (!administrador.isEsSuperAdmin()) {
            throw new RuntimeException("No tiene permisos de superadministrador");
        }

        return correo;
    }
}
