package proyecto.controladores;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import proyecto.dto.AdministradorDTO;
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
    public ResponseEntity<MensajeDTO<String>> crearVendedor(@Valid @RequestBody UsuarioDTO usuarioDTO)throws Exception{
        try {
            administradorServicio.crearVendedor(usuarioDTO);
            return ResponseEntity.ok().body(new MensajeDTO<>(false, "Se agregó el vendedor correctamente"));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest()
                    .body(new MensajeDTO<>(true, "La cédula es obligatoria y no puede estar vacía"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MensajeDTO<>(true, "Error interno al crear el vendedor"));
        }
    }

    @PostMapping("/crearAdministrador")
    public ResponseEntity<MensajeDTO<String>> crearAdministrador(@Valid @RequestBody AdministradorDTO administradorDTO)throws Exception{
        administradorServicio.crearAdministrador(administradorDTO);
        return ResponseEntity.ok().body(new MensajeDTO<>(false, "se agregó el administrador correctamente"));
    }
}