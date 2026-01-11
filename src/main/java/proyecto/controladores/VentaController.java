package proyecto.controladores;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.DetalleVentaResponseDTO;
import proyecto.dto.VentaRecuestDTO;
import proyecto.dto.VentaResponseDTO;
import proyecto.entidades.Venta;
import proyecto.servicios.interfaces.VentaServicio;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaServicio ventaService;

    @PostMapping
    public ResponseEntity<VentaResponseDTO> crearVenta(@RequestBody VentaRecuestDTO dto) {
        Venta venta = ventaService.crearVenta(dto);

        VentaResponseDTO response = mapToResponse(venta);

        return ResponseEntity.ok(response);
    }

    private VentaResponseDTO mapToResponse(Venta venta) {
        return new VentaResponseDTO(
                venta.getId(),
                venta.getFecha(),
                venta.getTotal(),
                venta.getVendedor().getNombre(),
                venta.getSede().getNombre(),
                venta.getDetalles().stream()
                        .map(d -> new DetalleVentaResponseDTO(
                                d.getProducto().getCodigo(),
                                d.getProducto().getNombre(),
                                d.getCantidad(),
                                d.getPrecioUnitario(),
                                d.getSubtotal()
                        ))
                        .toList()
        );
    }

    // 🔹 Mis ventas (todas)
    @GetMapping("/vendedor/{vendedorId}")
    public ResponseEntity<List<VentaResponseDTO>> misVentas(
            @PathVariable Long vendedorId
    ) {
        return ResponseEntity.ok(
                ventaService.listarVentasPorVendedor(vendedorId)
        );
    }

    // 🔹 Mis ventas por fecha
    @GetMapping("/vendedor/{vendedorId}/rango")
    public ResponseEntity<List<VentaResponseDTO>> misVentasPorFecha(
            @PathVariable Long vendedorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        return ResponseEntity.ok(
                ventaService.listarVentasPorVendedorEntreFechas(
                        vendedorId, desde, hasta
                )
        );
    }

    // 🔹 Ventas por sede (ADMIN)
    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<VentaResponseDTO>> ventasPorSede(@PathVariable Long sedeId) {
        return ResponseEntity.ok(
                ventaService.listarVentasPorSede(sedeId)
        );
    }

    // 🔹 Ventas por sede y fecha (ADMIN)
    @GetMapping("/sede/{sedeId}/rango")
    public ResponseEntity<List<VentaResponseDTO>> ventasPorSedePorFecha(@PathVariable Long sedeId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(
                ventaService.listarVentasPorSedeEntreFechas(
                        sedeId, desde, hasta
                )
        );
    }
}

