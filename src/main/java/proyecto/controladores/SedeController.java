package proyecto.controladores;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.MensajeDTO;
import proyecto.dto.SedeActualizarDTO;
import proyecto.dto.SedeCrearDTO;
import proyecto.dto.SedeDTO;
import proyecto.servicios.interfaces.SedeServicio;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/sedes")
@AllArgsConstructor
public class SedeController {
    private final SedeServicio sedeServicio;

    @PostMapping
    public ResponseEntity<SedeDTO> crear(@Valid @RequestBody SedeCrearDTO dto) {
        return ResponseEntity.ok(sedeServicio.crear(dto));
    }

    @GetMapping
    public ResponseEntity<List<SedeDTO>> listar() {
        return ResponseEntity.ok(sedeServicio.listar());
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
