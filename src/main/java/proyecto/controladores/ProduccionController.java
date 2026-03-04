package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.servicios.interfaces.ProduccionServicio;
import proyecto.utils.JWTUtils;

import java.util.List;

@RestController
@RequestMapping("/api/produccion")
@RequiredArgsConstructor
public class ProduccionController {

    private final ProduccionServicio produccionServicio;
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

    private String obtenerCorreo(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        return claims.getBody().getSubject();
    }
}
