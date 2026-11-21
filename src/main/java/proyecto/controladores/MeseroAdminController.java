package proyecto.controladores;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.MensajeDTO;
import proyecto.dto.MeseroDTO;
import proyecto.servicios.implementacion.MeseroCuentaService;

import java.util.List;

@RestController
@RequestMapping("/api/administrador/meseros/sede/{sedeId}")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class MeseroAdminController {
    private final MeseroCuentaService cuentas;

    @GetMapping
    public MensajeDTO<List<MeseroDTO.Cuenta>> listar(
            @RequestHeader("Authorization") String authorization, @PathVariable Long sedeId) {
        return new MensajeDTO<>(false, cuentas.listar(authorization, sedeId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeDTO<MeseroDTO.Cuenta> crear(
            @RequestHeader("Authorization") String authorization, @PathVariable Long sedeId,
            @Valid @RequestBody MeseroDTO.Alta dto) {
        return new MensajeDTO<>(false, cuentas.crear(authorization, sedeId, dto));
    }

    @PutMapping("/{meseroId}")
    public MensajeDTO<MeseroDTO.Cuenta> actualizar(
            @RequestHeader("Authorization") String authorization, @PathVariable Long sedeId,
            @PathVariable Long meseroId, @Valid @RequestBody MeseroDTO.Edicion dto) {
        return new MensajeDTO<>(false, cuentas.actualizar(authorization, sedeId, meseroId, dto));
    }
}
