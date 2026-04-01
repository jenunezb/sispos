package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.entidades.EstadoComandaCocina;
import proyecto.entidades.Venta;
import proyecto.servicios.interfaces.AdministradorServicio;
import proyecto.servicios.interfaces.ComandaCocinaServicio;
import proyecto.servicios.interfaces.ProduccionServicio;
import proyecto.servicios.interfaces.VentaServicio;
import proyecto.utils.JWTUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/produccion")
@RequiredArgsConstructor
public class ProduccionController {

    private final ProduccionServicio produccionServicio;
    private final VentaServicio ventaServicio;
    private final AdministradorServicio administradorServicio;
    private final ComandaCocinaServicio comandaCocinaServicio;
    private final JWTUtils jwtUtils;

    @PostMapping("/clientes")
    public ResponseEntity<ClienteDTO> crearCliente(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ClienteCrearDTO dto
    ) {
        return ResponseEntity.ok(
                produccionServicio.crearCliente(obtenerCorreo(authorization), dto)
        );
    }

    @GetMapping("/clientes")
    public ResponseEntity<List<ClienteDTO>> listarClientes(
            @RequestHeader("Authorization") String authorization
    ) {
        return ResponseEntity.ok(
                produccionServicio.listarClientes(obtenerCorreo(authorization))
        );
    }

    @GetMapping("/productos")
    public ResponseEntity<List<ProductoProduccionDTO>> listarProductos(
            @RequestHeader("Authorization") String authorization
    ) {
        return ResponseEntity.ok(
                produccionServicio.listarProductos(obtenerCorreo(authorization))
        );
    }

    @PostMapping("/inventario/produccion")
    public ResponseEntity<MensajeDTO<String>> registrarProduccion(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ProduccionRegistroDTO dto
    ) {
        return ResponseEntity.ok(new MensajeDTO<>(
                false,
                produccionServicio.registrarProduccion(obtenerCorreo(authorization), dto)
        ));
    }

    @GetMapping("/inventario")
    public ResponseEntity<List<InventarioProduccionDTO>> listarInventario(
            @RequestHeader("Authorization") String authorization
    ) {
        return ResponseEntity.ok(
                produccionServicio.listarInventario(obtenerCorreo(authorization))
        );
    }

    @PostMapping("/clientes/{clienteId}/precios")
    public ResponseEntity<PrecioClienteDTO> guardarPrecio(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long clienteId,
            @Valid @RequestBody PrecioClienteRequestDTO dto
    ) {
        return ResponseEntity.ok(
                produccionServicio.guardarPrecioCliente(obtenerCorreo(authorization), clienteId, dto)
        );
    }

    @GetMapping("/clientes/{clienteId}/precios")
    public ResponseEntity<List<PrecioClienteDTO>> listarPrecios(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long clienteId
    ) {
        return ResponseEntity.ok(
                produccionServicio.listarPreciosCliente(obtenerCorreo(authorization), clienteId)
        );
    }

    @PostMapping("/ventas")
    public ResponseEntity<VentaResponseDTO> crearVentaProduccion(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody VentaRecuestDTO dto
    ) {
        Venta venta = ventaServicio.crearVentaProduccion(obtenerCorreo(authorization), dto);
        return ResponseEntity.ok(ventaServicio.mapToResponse(venta));
    }

    @GetMapping("/ventas")
    public ResponseEntity<List<VentaResponseDTO>> listarVentas(
            @RequestHeader("Authorization") String authorization
    ) {
        return ResponseEntity.ok(
                produccionServicio.listarVentas(obtenerCorreo(authorization))
        );
    }

    @GetMapping("/ventas/rango")
    public ResponseEntity<List<VentaResponseDTO>> listarVentasRango(
            @RequestHeader("Authorization") String authorization,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        return ResponseEntity.ok(
                produccionServicio.listarVentasRango(obtenerCorreo(authorization), desde, hasta)
        );
    }

    @GetMapping("/informe-diario")
    public ResponseEntity<InformeProduccionDiaDTO> obtenerInformeDiario(
            @RequestHeader("Authorization") String authorization,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        return ResponseEntity.ok(
                produccionServicio.obtenerInformeDiario(obtenerCorreo(authorization), fecha)
        );
    }

    @GetMapping("/logo")
    public ResponseEntity<MensajeDTO<String>> obtenerLogoEmpresa(
            @RequestHeader("Authorization") String authorization
    ) {
        String correo = obtenerCorreo(authorization);
        return ResponseEntity.ok(new MensajeDTO<>(false, administradorServicio.obtenerLogoEmpresa(correo)));
    }

    @GetMapping("/impresion-cocina")
    public ResponseEntity<MensajeDTO<Boolean>> obtenerImpresionCocina(
            @RequestHeader("Authorization") String authorization
    ) {
        String correo = obtenerCorreo(authorization);
        return ResponseEntity.ok(new MensajeDTO<>(false, administradorServicio.obtenerImpresionCocinaHabilitada(correo)));
    }

    @GetMapping("/comandas-cocina")
    public ResponseEntity<MensajeDTO<List<ComandaCocinaResponseDTO>>> listarComandasCocina(
            @RequestHeader("Authorization") String authorization
    ) {
        String correo = obtenerCorreo(authorization);
        return ResponseEntity.ok(new MensajeDTO<>(false, comandaCocinaServicio.listarComandasActivas(correo)));
    }

    @PatchMapping("/comandas-cocina/{comandaId}/estado")
    public ResponseEntity<MensajeDTO<ComandaCocinaResponseDTO>> actualizarEstadoComanda(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long comandaId,
            @Valid @RequestBody ComandaCocinaEstadoDTO dto
    ) {
        String correo = obtenerCorreo(authorization);
        return ResponseEntity.ok(new MensajeDTO<>(
                false,
                comandaCocinaServicio.actualizarEstado(correo, comandaId, dto.estado())
        ));
    }

    private String obtenerCorreo(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        return claims.getBody().getSubject();
    }
}
