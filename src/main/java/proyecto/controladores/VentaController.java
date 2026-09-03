package proyecto.controladores;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.ComandaCocinaCrearDTO;
import proyecto.dto.ComandaCocinaResponseDTO;
import proyecto.dto.VentaRecuestDTO;
import proyecto.dto.VentaResponseDTO;
import proyecto.servicios.implementacion.VentaAccesoService;
import proyecto.utils.JWTUtils;
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
    private final VentaAccesoService accesoService;
    private final JWTUtils jwtUtils;

    @PostMapping
    public ResponseEntity<VentaResponseDTO> crearVenta(
            @RequestHeader("Authorization") String authorization, @RequestBody VentaRecuestDTO dto) {
        var claims = jwtUtils.parseJwt(authorization.replace("Bearer ", "")).getBody();
        String correo = claims.getSubject();
        Venta venta;
        if ("produccion".equals(claims.get("rol", String.class))) {
            venta = ventaService.crearVentaProduccion(correo, dto);
        } else {
            accesoService.validarSede(authorization, dto.sedeId());
            VentaRecuestDTO autenticada = new VentaRecuestDTO(correo, dto.sedeId(), dto.clienteId(),
                    dto.detalles(), dto.modoPago(), dto.montoRecibido(), dto.montoEfectivo(), dto.montoTransferencia());
            venta = ventaService.crearVentaAutenticada(autenticada, claims.get("rol", String.class));
        }
        return ResponseEntity.ok( ventaService.mapToResponse(venta));
    }

    @PostMapping("/comandas-cocina")
    public ResponseEntity<ComandaCocinaResponseDTO> crearComandaCocina(
            @RequestBody ComandaCocinaCrearDTO dto
    ) {
        return ResponseEntity.ok(comandaCocinaServicio.crearComanda(dto));
    }

    @GetMapping("/{ventaId}")
    public ResponseEntity<VentaResponseDTO> obtenerVentaPorId(@RequestHeader("Authorization") String authorization, @PathVariable Long ventaId) {
        accesoService.validarVenta(authorization, ventaId);
        return ResponseEntity.ok( ventaService.obtenerVentaPorId(ventaId));
    }

    @GetMapping("/vendedor/{vendedorId}")
    public ResponseEntity<List<VentaResponseDTO>> misVentas(@RequestHeader("Authorization") String authorization, @PathVariable Long vendedorId) {
        accesoService.validarVendedor(authorization, vendedorId);
        return ResponseEntity.ok( accesoService.validarResultados(authorization, ventaService.listarVentasPorVendedor(vendedorId)));
    }

    @GetMapping("/vendedor/{vendedorId}/rango")
    public ResponseEntity<List<VentaResponseDTO>> misVentasPorFecha(@RequestHeader("Authorization") String authorization,
            @PathVariable Long vendedorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        accesoService.validarVendedor(authorization, vendedorId);
        return ResponseEntity.ok(
                accesoService.validarResultados(authorization, ventaService.listarVentasPorVendedorEntreFechas(vendedorId, desde, hasta))
        );
    }

    @GetMapping("/vendedor/correo/{correo}")
    public ResponseEntity<List<VentaResponseDTO>> misVentasPorCorreo(@RequestHeader("Authorization") String authorization, @PathVariable String correo) {
        accesoService.validarCorreoVendedor(authorization, correo);
        return ResponseEntity.ok( accesoService.validarResultados(authorization, ventaService.listarVentasPorCorreoVendedor(correo)));
    }

    @GetMapping("/vendedor/correo/{correo}/rango")
    public ResponseEntity<List<VentaResponseDTO>> misVentasPorCorreoYFecha(@RequestHeader("Authorization") String authorization,
            @PathVariable String correo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        accesoService.validarCorreoVendedor(authorization, correo);
        return ResponseEntity.ok(
                accesoService.validarResultados(authorization, ventaService.listarVentasPorCorreoVendedorEntreFechas(correo, desde, hasta))
        );
    }

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<VentaResponseDTO>> ventasPorSede(@RequestHeader("Authorization") String authorization, @PathVariable Long sedeId) {
        accesoService.validarSede(authorization, sedeId);
        return ResponseEntity.ok( ventaService.listarVentasPorSede(sedeId));
    }

    @GetMapping("/sede/{sedeId}/rango")
    public ResponseEntity<List<VentaResponseDTO>> ventasPorSedePorFecha(@RequestHeader("Authorization") String authorization,
            @PathVariable Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        accesoService.validarSede(authorization, sedeId);
        return ResponseEntity.ok(
                ventaService.listarVentasPorSedeEntreFechas(sedeId, desde, hasta)
        );
    }

    @GetMapping("/sede/{sedeId}/anuladas")
    public ResponseEntity<List<VentaResponseDTO>> ventasAnuladasPorSede(@RequestHeader("Authorization") String authorization, @PathVariable Long sedeId) {
        accesoService.validarSede(authorization, sedeId);
        return ResponseEntity.ok( ventaService.listarVentasAnuladas(sedeId));
    }

    @GetMapping("/sede/{sedeId}/anuladas/rango")
    public ResponseEntity<List<VentaResponseDTO>> ventasAnuladasPorSedePorFecha(@RequestHeader("Authorization") String authorization,
            @PathVariable Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        accesoService.validarSede(authorization, sedeId);
        return ResponseEntity.ok(
                ventaService.listarVentasAnuladasEntreFechas(sedeId, desde, hasta)
        );
    }
}
