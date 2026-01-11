package proyecto.controladores;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.MateriaPrimaRequestDTO;
import proyecto.dto.ProductoMateriaPrimaRequestDTO;
import proyecto.entidades.MateriaPrima;
import proyecto.entidades.ProductoMateriaPrima;
import proyecto.servicios.implementacion.MateriaPrimaSedeServiceImpl;

@RestController
@RequestMapping("/api/materias-primas")
@RequiredArgsConstructor
public class MateriaPrimaController {

    private final MateriaPrimaSedeServiceImpl materiaPrimaSedeService;

    /**
     * Crear una nueva materia prima
     */
    @PostMapping
    public ResponseEntity<MateriaPrima> crear( @Valid @RequestBody MateriaPrimaRequestDTO dto, @RequestParam Long sedeId) {
        MateriaPrima materiaPrima = materiaPrimaSedeService.crearMateriaPrima(dto, sedeId);
        return ResponseEntity.status(HttpStatus.CREATED).body(materiaPrima);
    }

    /**
     * Vincular la materia prima con un producto específico
     */
    @PostMapping("/vincular/{productoId}/{materiaPrimaId}")
    public ResponseEntity<ProductoMateriaPrimaRequestDTO> vincular(
            @PathVariable Long productoId,
            @PathVariable Long materiaPrimaId,
            @RequestParam double mlConsumidos
    ) {
        ProductoMateriaPrimaRequestDTO dto = materiaPrimaSedeService.vincularMateriaPrima(productoId, materiaPrimaId, mlConsumidos);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Ajustar cantidad de materia prima en una sede
     */
    @PostMapping("/{materiaPrimaId}/sede/{sedeId}/ajustar")
    public ResponseEntity<String> ajustarCantidad(
            @PathVariable Long materiaPrimaId,
            @PathVariable Long sedeId,
            @RequestParam double ml
    ) {
        materiaPrimaSedeService.ajustarCantidad(materiaPrimaId, sedeId, ml);
        return ResponseEntity.ok("Cantidad ajustada correctamente");
    }
}
