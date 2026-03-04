package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.entidades.Venta;
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

    @PostMapping("/inventario/produccion")
    public ResponseEntity<Void> registrarProduccion(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ProduccionRegistroDTO dto
    ) {
        produccionServicio.registrarProduccion(obtenerCorreo(authorization), dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/inventario")
    public ResponseEntity<List<InventarioProduccionDTO>> listarInventarioProduccion(
            @RequestHeader("Authorization") String authorization
    ) {
        return ResponseEntity.ok(
                produccionServicio.listarInventarioProduccion(obtenerCorreo(authorization))
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

    @PostMapping("/ventas")
    public ResponseEntity<VentaResponseDTO> crearVentaProduccion(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody VentaRecuestDTO dto
    ) {
        Venta venta = ventaServicio.crearVentaProduccion(obtenerCorreo(authorization), dto);
        return ResponseEntity.ok(ventaServicio.mapToResponse(venta));
    }

    @GetMapping("/ventas")
    public ResponseEntity<List<VentaResponseDTO>> listarVentasProduccion(
            @RequestHeader("Authorization") String authorization
    ) {
        return ResponseEntity.ok(
                ventaServicio.listarVentasPorCorreoVendedor(obtenerCorreo(authorization))
        );
    }

    @GetMapping("/ventas/rango")
    public ResponseEntity<List<VentaResponseDTO>> listarVentasProduccionPorRango(
            @RequestHeader("Authorization") String authorization,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta
    ) {
        return ResponseEntity.ok(
                ventaServicio.listarVentasPorCorreoVendedorEntreFechas(obtenerCorreo(authorization), desde, hasta)
        );
    }

    private String obtenerCorreo(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        return claims.getBody().getSubject();
    }
}
