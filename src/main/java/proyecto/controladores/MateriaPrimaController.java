package proyecto.controladores;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.entidades.MateriaPrima;
import proyecto.entidades.ProductoMateriaPrima;
import proyecto.servicios.implementacion.MateriaPrimaSedeServiceImpl;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/materias-primas")
@RequiredArgsConstructor
public class MateriaPrimaController {

    private final MateriaPrimaSedeServiceImpl materiaPrimaSedeService;

    /**
     * Crear una nueva materia prima
     */
    @PostMapping
    public ResponseEntity<Void> crear(@Valid @RequestBody CrearMateriaPrimaDTO dto) {
        materiaPrimaSedeService.crearMateriaPrima(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Vincula una materia prima con una sede
     */
    @PostMapping("/{materiaPrimaId}/sedes/{sedeId}")
    public ResponseEntity<Map<String, String>> vincularMateriaPrimaSede(@PathVariable Long materiaPrimaId, @PathVariable Long sedeId) {

        materiaPrimaSedeService.materiaPrimaSede(materiaPrimaId, sedeId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "mensaje", "Materia prima vinculada correctamente a la sede"
                ));
    }

    //Listar Todas las materias primas
    @GetMapping
    public ResponseEntity<List<MateriaPrimaSedeDTO>> listarTodas() {
        return ResponseEntity.ok(materiaPrimaSedeService.listarTodas());
    }

    /**
     * Vincular la materia prima con un producto específico
     */
    @PostMapping("/vincular/{productoId}/{materiaPrimaId}")
    public ResponseEntity<ProductoMateriaPrimaRequestDTO> vincular(@PathVariable Long productoId, @PathVariable Long materiaPrimaId, @RequestParam double mlConsumidos) {
        ProductoMateriaPrimaRequestDTO dto = materiaPrimaSedeService.vincularMateriaPrima(productoId, materiaPrimaId, mlConsumidos);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    //Crear y vincular materia prima a una sede
    @PostMapping("/crear-y-vincular")
    public ResponseEntity<MateriaPrimaSedeResponseDTO> crearYVincular( @Valid @RequestBody CrearMateriaPrimaSedeDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materiaPrimaSedeService.crearYVincular(dto));
    }

    /**
     * Ajustar cantidad de materia prima en una sede
     */
    @PostMapping("/{materiaPrimaId}/sede/{sedeId}/ajustar")
    public ResponseEntity<String> ajustarCantidad( @PathVariable Long materiaPrimaId, @PathVariable Long sedeId,@RequestParam double ml) {
        materiaPrimaSedeService.ajustarCantidad(materiaPrimaId, sedeId, ml);
        return ResponseEntity.ok("Cantidad ajustada correctamente");
    }
}
