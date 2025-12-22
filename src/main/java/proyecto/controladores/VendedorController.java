package proyecto.controladores;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import proyecto.dto.InventarioDTO;
import proyecto.entidades.Usuario;
import proyecto.entidades.Vendedor;
import proyecto.servicios.interfaces.InventarioServicio;
import proyecto.servicios.interfaces.VendedorServicio;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/vendedor")
@AllArgsConstructor
public class VendedorController {

    private InventarioServicio inventarioServicio;
    private VendedorServicio vendedorServicio;

    @GetMapping("/inventario")
    public List<InventarioDTO> inventarioVendedor(
            String correo) {

        Vendedor vendedor =vendedorServicio.obtenerVendedorPorCorreo(correo);
        Long sedeId = vendedor.getSede().getId();
        return inventarioServicio.listarPorSede(sedeId);
    }

}
