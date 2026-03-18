package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.MensajeDTO;
import proyecto.dto.SedeActualizarDTO;
import proyecto.dto.SedeCrearDTO;
import proyecto.dto.SedeDTO;
import proyecto.entidades.Administrador;
import proyecto.repositorios.AdministradorRepository;
import proyecto.servicios.interfaces.SedeServicio;
import proyecto.utils.JWTUtils;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/sedes")
@AllArgsConstructor
public class SedeController {
    private final SedeServicio sedeServicio;
    private final JWTUtils jwtUtils;
    private final AdministradorRepository administradorRepository;

    @PostMapping
    public ResponseEntity<SedeDTO> crear(@Valid @RequestBody SedeCrearDTO dto) {
        return ResponseEntity.ok(sedeServicio.crear(dto));
    }

    @GetMapping
    public ResponseEntity<List<SedeDTO>> listar(@RequestHeader("Authorization") String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        String correo = claims.getBody().getSubject();
        String rol = (String) claims.getBody().get("rol");

        if (!"administrador".equals(rol)) {
            throw new RuntimeException("No tiene permisos para listar sedes");
        }

        Administrador admin = administradorRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));

        if (admin.isEsSuperAdmin()) {
            return ResponseEntity.ok(sedeServicio.listar());
        }

        return ResponseEntity.ok(sedeServicio.listarPorEmpresa(admin.getEmpresa().getNit()));
    }

    @PutMapping
    public ResponseEntity<SedeDTO> actualizar(@Valid @RequestBody SedeActualizarDTO dto) {
        return ResponseEntity.ok(sedeServicio.actualizar(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MensajeDTO<SedeDTO>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(
                new MensajeDTO<>(false, sedeServicio.obtenerPorId(id))
        );
    }
}
