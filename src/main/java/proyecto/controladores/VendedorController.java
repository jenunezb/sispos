package proyecto.controladores;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import proyecto.servicios.interfaces.VendedorServicio;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/vendedor")
@AllArgsConstructor
public class VendedorController {


}
