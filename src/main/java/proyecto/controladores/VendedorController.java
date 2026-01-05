package proyecto.controladores;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.BalanceSedeDTO;
import proyecto.dto.BalanceSedeVendedor;
import proyecto.dto.InventarioDTO;
import proyecto.dto.InventarioVendedorResponseDTO;
import proyecto.entidades.Usuario;
import proyecto.entidades.Vendedor;
import proyecto.servicios.interfaces.AdministradorServicio;
import proyecto.servicios.interfaces.InventarioServicio;
import proyecto.servicios.interfaces.VendedorServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/vendedor")
@AllArgsConstructor
public class VendedorController {

    private InventarioServicio inventarioServicio;
    private VendedorServicio vendedorServicio;

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
}



