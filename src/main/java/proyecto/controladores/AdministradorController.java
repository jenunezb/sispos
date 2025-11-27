package proyecto.controladores;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import proyecto.dto.MensajeDTO;
import proyecto.dto.UsuarioDTO;
import proyecto.servicios.interfaces.AdministradorServicio;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/administrador")
@AllArgsConstructor

public class AdministradorController {
    private final AdministradorServicio administradorServicio;

    @PostMapping("/agregarVendedor")
    public ResponseEntity<MensajeDTO<String>> crearDigitador(@Valid @RequestBody UsuarioDTO usuarioDTO)throws Exception{
        administradorServicio.crearVendedor(usuarioDTO);
        return ResponseEntity.ok().body(new MensajeDTO<>(false, "se agregó el digitador correctamente"));
    }
}
