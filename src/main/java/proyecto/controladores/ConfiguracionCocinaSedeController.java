package proyecto.controladores;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.ConfigImpresionCocinaDTO;
import proyecto.dto.ImpresionCocinaSedeDTO;
import proyecto.dto.MensajeDTO;
import proyecto.servicios.implementacion.ConfiguracionCocinaSedeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sedes/{sedeId}/configuracion/impresion-cocina")
public class ConfiguracionCocinaSedeController {
    private final ConfiguracionCocinaSedeService service;

    @GetMapping
    public MensajeDTO<ImpresionCocinaSedeDTO> obtener(
            @RequestHeader("Authorization") String authorization, @PathVariable Long sedeId) {
        return new MensajeDTO<>(false, service.obtener(authorization, sedeId));
    }

    @PutMapping
    public MensajeDTO<ImpresionCocinaSedeDTO> actualizar(
            @RequestHeader("Authorization") String authorization, @PathVariable Long sedeId,
            @Valid @RequestBody ConfigImpresionCocinaDTO dto) {
        return new MensajeDTO<>(false, service.actualizar(authorization, sedeId, dto.habilitada()));
    }
}
