package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.ComandaCocinaEstadoDTO;
import proyecto.dto.ComandaCocinaResponseDTO;
import proyecto.dto.MensajeDTO;
import proyecto.servicios.interfaces.ComandaCocinaServicio;
import proyecto.utils.JWTUtils;

import java.util.List;

@RestController
@RequestMapping("/api/cocina")
@RequiredArgsConstructor
public class CocinaController {
    private final ComandaCocinaServicio comandaCocinaServicio;
    private final JWTUtils jwtUtils;

    @GetMapping("/comandas")
    public ResponseEntity<MensajeDTO<List<ComandaCocinaResponseDTO>>> listar(
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(new MensajeDTO<>(false,
                comandaCocinaServicio.listarComandasActivas(obtenerCorreo(authorization))));
    }

    @PatchMapping("/comandas/{comandaId}/estado")
    public ResponseEntity<MensajeDTO<ComandaCocinaResponseDTO>> actualizarEstado(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long comandaId,
            @Valid @RequestBody ComandaCocinaEstadoDTO dto) {
        return ResponseEntity.ok(new MensajeDTO<>(false,
                comandaCocinaServicio.actualizarEstado(obtenerCorreo(authorization), comandaId, dto.estado())));
    }

    private String obtenerCorreo(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        return claims.getBody().getSubject();
    }
}
