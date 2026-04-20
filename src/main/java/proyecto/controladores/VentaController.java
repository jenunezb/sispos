package proyecto.controladores;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.ComandaCocinaCrearDTO;
import proyecto.dto.ComandaCocinaResponseDTO;
import proyecto.dto.VentaRecuestDTO;
import proyecto.dto.VentaResponseDTO;
import proyecto.entidades.Venta;
import proyecto.servicios.interfaces.ComandaCocinaServicio;
import proyecto.servicios.interfaces.VentaServicio;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaServicio ventaService;
    private final ComandaCocinaServicio comandaCocinaServicio;

    @PostMapping
    public ResponseEntity<VentaResponseDTO> crearVenta(@RequestBody VentaRecuestDTO dto) {
        Venta venta = ventaService.crearVenta(dto);
        return ResponseEntity.ok(ventaService.mapToResponse(venta));
    }

    @PostMapping("/comandas-cocina")
    public ResponseEntity<ComandaCocinaResponseDTO> crearComandaCocina(
            @RequestBody ComandaCocinaCrearDTO dto
    ) {
        return ResponseEntity.ok(comandaCocinaServicio.crearComanda(dto));
    }

    @GetMapping("/{ventaId}")
    public ResponseEntity<VentaResponseDTO> obtenerVentaPorId(@PathVariable Long ventaId) {
        return ResponseEntity.ok(ventaService.obtenerVentaPorId(ventaId));
    }

    @GetMapping("/vendedor/{vendedorId}")
    public ResponseEntity<List<VentaResponseDTO>> misVentas(@PathVariable Long vendedorId) {
        return ResponseEntity.ok(ventaService.listarVentasPorVendedor(vendedorId));
    }

    @GetMapping("/vendedor/{vendedorId}/rango")
    public ResponseEntity<List<VentaResponseDTO>> misVentasPorFecha(
            @PathVariable Long vendedorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        return ResponseEntity.ok(
                ventaService.listarVentasPorVendedorEntreFechas(vendedorId, desde, hasta)
        );
    }

    @GetMapping("/vendedor/correo/{correo}")
    public ResponseEntity<List<VentaResponseDTO>> misVentasPorCorreo(@PathVariable String correo) {
        return ResponseEntity.ok(ventaService.listarVentasPorCorreoVendedor(correo));
    }

    @GetMapping("/vendedor/correo/{correo}/rango")
    public ResponseEntity<List<VentaResponseDTO>> misVentasPorCorreoYFecha(
            @PathVariable String correo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        return ResponseEntity.ok(
                ventaService.listarVentasPorCorreoVendedorEntreFechas(correo, desde, hasta)
        );
    }

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<VentaResponseDTO>> ventasPorSede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(ventaService.listarVentasPorSede(sedeId));
    }

    @GetMapping("/sede/{sedeId}/rango")
    public ResponseEntity<List<VentaResponseDTO>> ventasPorSedePorFecha(
            @PathVariable Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        return ResponseEntity.ok(
                ventaService.listarVentasPorSedeEntreFechas(sedeId, desde, hasta)
        );
    }

    @GetMapping("/sede/{sedeId}/anuladas")
    public ResponseEntity<List<VentaResponseDTO>> ventasAnuladasPorSede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(ventaService.listarVentasAnuladas(sedeId));
    }

    @GetMapping("/sede/{sedeId}/anuladas/rango")
    public ResponseEntity<List<VentaResponseDTO>> ventasAnuladasPorSedePorFecha(
            @PathVariable Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        return ResponseEntity.ok(
                ventaService.listarVentasAnuladasEntreFechas(sedeId, desde, hasta)
        );
    }
}
