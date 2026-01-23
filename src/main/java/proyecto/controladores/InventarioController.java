package proyecto.controladores;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.servicios.interfaces.InventarioServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inventarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InventarioController {

    private final InventarioServicio inventarioServicio;

    // ===============================
    // LISTAR INVENTARIO POR SEDE
    // ===============================
    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<InventarioDTO>> listarPorSede(
            @PathVariable Long sedeId
    ) {
        return ResponseEntity.ok(
                inventarioServicio.listarPorSede(sedeId)
        );
    }

    @GetMapping("/sed/{sedeId}")
    public ResponseEntity<List<InventarioDTO>> listarPorSede1(@PathVariable Long sedeId) {
        return ResponseEntity.ok(inventarioServicio.listarPorSede(sedeId));
    }

    // ===============================
    // OBTENER INVENTARIO POR PRODUCTO Y SEDE
    // ===============================
    @GetMapping("/producto/{productoId}/sede/{sedeId}")
    public ResponseEntity<InventarioDTO> obtenerPorProductoYSede(
            @PathVariable Long productoId,
            @PathVariable Long sedeId
    ) {
        return ResponseEntity.ok(
                inventarioServicio.obtenerPorProductoYSede(productoId, sedeId)
        );
    }

    // ===============================
    // REGISTRAR ENTRADA
    // ===============================
    @PostMapping("/entrada")
    public ResponseEntity<Void> registrarEntrada(
            @RequestParam Long productoId,
            @RequestParam Long sedeId,
            @RequestParam Integer cantidad
    ) {
        inventarioServicio.registrarEntrada(productoId, sedeId, cantidad);
        return ResponseEntity.ok().build();
    }

    // ===============================
    // REGISTRAR SALIDA (VENTA)
    // ===============================
    @PostMapping("/salida")
    public ResponseEntity<Void> registrarSalida(
            @RequestParam Long productoId,
            @RequestParam Long sedeId,
            @RequestParam Integer cantidad
    ) {
        inventarioServicio.registrarSalida(productoId, sedeId, cantidad, "observacion");
        return ResponseEntity.ok().build();
    }

    // ===============================
    // REGISTRAR PÉRDIDA
    // ===============================
    @PostMapping("/perdida")
    public ResponseEntity<Void> registrarPerdida(
            @RequestParam Long productoId,
            @RequestParam Long sedeId,
            @RequestParam Integer cantidad
    ) {
        inventarioServicio.registrarPerdida(productoId, sedeId, cantidad);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/movimiento")
    public ResponseEntity<Void> registrarMovimiento(
            @RequestBody MovimientoInventarioDTO dto) {

        inventarioServicio.registrarMovimiento(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/perdidas/detalle")
    public ResponseEntity<List<PerdidasDetalleDTO>> obtenerPerdidasDetalle(
            @RequestParam Long sedeId,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin
    ) {
        LocalDateTime inicio = LocalDate.parse(fechaInicio).atStartOfDay();
        LocalDateTime fin = LocalDate.parse(fechaFin).atTime(23, 59, 59);

        return ResponseEntity.ok(
                inventarioServicio.obtenerPerdidasDetalladasPorRango(
                        sedeId, inicio, fin
                )
        );
    }

    // ===============================
    // INVENTARIO DEL DÍA
    // ===============================
    @GetMapping("/dia")
    public ResponseEntity<List<InventarioDelDia>> obtenerInventarioDelDia(
            @RequestParam Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {

        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        return ResponseEntity.ok(
                inventarioServicio.obtenerInventarioDia(
                        sedeId,
                        inicio,
                        fin
                )
        );
    }

    @GetMapping("/materia-prima/dia")
    public ResponseEntity<List<MateriaPrimaInventarioDTO>> inventarioMateriaPrimaDia(
            @RequestParam Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(23, 59, 59);

        return ResponseEntity.ok(
                inventarioServicio.obtenerInventarioMateriaPrimaDia(
                        sedeId, inicio, fin
                )
        );
    }

    @PostMapping("/materia-prima/movimiento")
    public ResponseEntity<Void> registrarMovimientoMateriaPrima(
            @RequestBody MovimientoMateriaPrimaDTO dto
    ) {
        inventarioServicio.registrarMovimientoMateriaPrima(dto);
        return ResponseEntity.ok().build();
    }

}
