package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.BalanceSedeVendedor;
import proyecto.dto.InventarioDTO;
import proyecto.dto.InventarioVendedorResponseDTO;
import proyecto.dto.MensajeDTO;
import proyecto.entidades.Vendedor;
import proyecto.servicios.interfaces.AdministradorServicio;
import proyecto.servicios.interfaces.InventarioServicio;
import proyecto.servicios.interfaces.VendedorServicio;
import proyecto.utils.JWTUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/vendedor")
@AllArgsConstructor
public class VendedorController {

    private final InventarioServicio inventarioServicio;
    private final VendedorServicio vendedorServicio;
    private final AdministradorServicio administradorServicio;
    private final JWTUtils jwtUtils;

    @GetMapping("/inventario")
    public InventarioVendedorResponseDTO inventarioVendedor(@RequestParam("correo") String correo) {

        Vendedor vendedor = vendedorServicio.obtenerVendedorPorCorreo(correo);
        Long sedeId = vendedor.getSede().getId();

        List<InventarioDTO> inventario = inventarioServicio.listarPorSede(sedeId);

        return new InventarioVendedorResponseDTO(sedeId, inventario);
    }

    @GetMapping("/sede")
    public BalanceSedeVendedor balanceMiSede(
            @RequestParam String correo,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta
    ) {

        LocalDateTime fDesde;
        LocalDateTime fHasta;

        if (desde != null && hasta != null) {
            fDesde = LocalDate.parse(desde).atStartOfDay();
            fHasta = LocalDate.parse(hasta).atTime(23, 59, 59);
        } else {
            fDesde = LocalDate.now().atStartOfDay();
            fHasta = LocalDate.now().atTime(23, 59, 59);
        }

        return vendedorServicio.balancePorSedeId(correo, fDesde, fHasta);
    }

    @GetMapping("/logo")
    public ResponseEntity<MensajeDTO<String>> obtenerLogoEmpresa(
            @RequestHeader("Authorization") String authorization
    ) {
        String correo = obtenerCorreo(authorization);
        return ResponseEntity.ok(new MensajeDTO<>(false, administradorServicio.obtenerLogoEmpresa(correo)));
    }

    private String obtenerCorreo(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        return claims.getBody().getSubject();
    }
}



