package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.MensajeDTO;
import proyecto.dto.MesaEstadoDTO;
import proyecto.servicios.interfaces.MesaEstadoServicio;
import proyecto.utils.JWTUtils;

import java.util.List;

@RestController
@RequestMapping("/api/mesas")
@RequiredArgsConstructor
public class MesaEstadoController {

    private final MesaEstadoServicio mesaEstadoServicio;
    private final JWTUtils jwtUtils;

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<MensajeDTO<List<MesaEstadoDTO>>> listarPorSede(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long sedeId
    ) {
        SessionData sessionData = obtenerSessionData(authorization);
        return ResponseEntity.ok(new MensajeDTO<>(
                false,
                mesaEstadoServicio.listarPorSede(sessionData.correo(), sessionData.rol(), sedeId)
        ));
    }

    @PutMapping("/sede/{sedeId}/{mesaId}")
    public ResponseEntity<MensajeDTO<MesaEstadoDTO>> guardarMesa(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long sedeId,
            @PathVariable Long mesaId,
            @Valid @RequestBody MesaEstadoDTO dto
    ) {
        SessionData sessionData = obtenerSessionData(authorization);
        return ResponseEntity.ok(new MensajeDTO<>(
                false,
                mesaEstadoServicio.guardarMesa(sessionData.correo(), sessionData.rol(), sedeId, mesaId, dto)
        ));
    }

    private SessionData obtenerSessionData(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        return new SessionData(
                claims.getBody().getSubject(),
                String.valueOf(claims.getBody().get("rol"))
        );
    }

    private record SessionData(String correo, String rol) {}
}
