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
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.entidades.Administrador;
import proyecto.entidades.Sede;
import proyecto.repositorios.SedeRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/materias-primas")
@RequiredArgsConstructor
public class MateriaPrimaController {

    private final MateriaPrimaSedeServiceImpl materiaPrimaSedeService;
    private final AdministradorAccesoService administradorAccesoService;
    private final SedeRepository sedeRepository;

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
    public ResponseEntity<List<MateriaPrimaSedeDTO>> listarTodas(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit
    ) {
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        List<Sede> sedes = admin.isEsSuperAdmin()
                ? sedeRepository.findByEmpresaNit(administradorAccesoService.resolverEmpresaNit(admin, empresaNit))
                : administradorAccesoService.obtenerSedesVisibles(admin);
        return ResponseEntity.ok(materiaPrimaSedeService.listarPorSedes(
                sedes.stream().map(Sede::getId).toList()
        ));
    }

    @PostMapping("/carga-masiva")
    public ResponseEntity<List<CargaMateriaPrimaResultadoDTO>> cargarMasivamente(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CargaMateriaPrimaMasivaDTO dto
    ) {
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        administradorAccesoService.validarAccesoASede(admin, dto.sedeId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materiaPrimaSedeService.cargarMasivamente(dto));
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
     * Actualizar cantidad, ml por vaso y estado activo/inactivo
     */
    @PutMapping("/{id}")
    public ResponseEntity<String> actualizarMateriaPrimaSede(
            @PathVariable Long id,
            @RequestBody MateriaPrimaSedeUpdate dto
    ) {
        materiaPrimaSedeService.actualizarMateriaPrimaSede(id, dto);
        return ResponseEntity.ok("Materia prima actualizada correctamente");
    }

    /** Elimina definitivamente una materia prima y todas sus vinculaciones. */
    @DeleteMapping("/{materiaPrimaId}")
    public ResponseEntity<Map<String, String>> eliminarMateriaPrima(@PathVariable Long materiaPrimaId) {
        materiaPrimaSedeService.eliminarMateriaPrima(materiaPrimaId);
        return ResponseEntity.ok(Map.of("mensaje", "Materia prima eliminada definitivamente"));
    }

    /**
     * Vincular un producto a una materia prima en una sede
     */
    @PostMapping("/vincular-producto")
    public ResponseEntity<Map<String, String>> vincularProducto(
            @RequestBody VincularProductoDTO dto
    ) {
        materiaPrimaSedeService.vincularProducto(dto);
        return ResponseEntity.ok(Map.of("mensaje", "Producto vinculado correctamente"));
    }

    @PutMapping("/{materiaPrimaSedeId}/productos/{productoId}")
    public ResponseEntity<Map<String, String>> actualizarConsumoProducto(
            @PathVariable Long materiaPrimaSedeId,
            @PathVariable Long productoId,
            @Valid @RequestBody ActualizarConsumoProductoDTO dto
    ) {
        materiaPrimaSedeService.actualizarConsumoProducto(materiaPrimaSedeId, productoId, dto);
        return ResponseEntity.ok(Map.of("mensaje", "Consumo del producto actualizado correctamente"));
    }

    @GetMapping("/{materiaPrimaSedeId}/productos")
    public ResponseEntity<List<MateriaPrimaProductoDTO>> listarProductosVinculados(
            @PathVariable Long materiaPrimaSedeId
    ) {
        return ResponseEntity.ok(materiaPrimaSedeService.listarProductosVinculados(materiaPrimaSedeId));
    }

    @DeleteMapping("/{materiaPrimaSedeId}/productos/{productoId}")
    public ResponseEntity<Map<String, String>> desvincularProducto(
            @PathVariable Long materiaPrimaSedeId,
            @PathVariable Long productoId
    ) {
        materiaPrimaSedeService.desvincularProducto(materiaPrimaSedeId, productoId);
        return ResponseEntity.ok(Map.of("mensaje", "Producto desvinculado correctamente"));
    }

    @GetMapping("/productos/{productoId}/ingredientes")
    public ResponseEntity<List<IngredienteProductoDTO>> listarIngredientesProducto(
            @PathVariable Long productoId
    ) {
        return ResponseEntity.ok(materiaPrimaSedeService.listarIngredientesProducto(productoId));
    }

    @PostMapping("/productos/{productoId}/ingredientes/{materiaPrimaId}")
    public ResponseEntity<ProductoMateriaPrimaRequestDTO> agregarIngredienteProducto(
            @PathVariable Long productoId,
            @PathVariable Long materiaPrimaId,
            @RequestParam double mlConsumidos
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                materiaPrimaSedeService.vincularMateriaPrima(productoId, materiaPrimaId, mlConsumidos)
        );
    }

    @PutMapping("/productos/{productoId}/ingredientes/{materiaPrimaId}")
    public ResponseEntity<Map<String, String>> actualizarIngredienteProducto(
            @PathVariable Long productoId,
            @PathVariable Long materiaPrimaId,
            @Valid @RequestBody ActualizarConsumoProductoDTO dto
    ) {
        materiaPrimaSedeService.actualizarIngredienteProducto(productoId, materiaPrimaId, dto);
        return ResponseEntity.ok(Map.of("mensaje", "Ingrediente actualizado correctamente"));
    }

    @DeleteMapping("/productos/{productoId}/ingredientes/{materiaPrimaId}")
    public ResponseEntity<Map<String, String>> eliminarIngredienteProducto(
            @PathVariable Long productoId,
            @PathVariable Long materiaPrimaId
    ) {
        materiaPrimaSedeService.eliminarIngredienteProducto(productoId, materiaPrimaId);
        return ResponseEntity.ok(Map.of("mensaje", "Ingrediente retirado correctamente"));
    }

}
