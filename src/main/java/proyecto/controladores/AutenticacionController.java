package proyecto.controladores;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.servicios.interfaces.AdministradorServicio;
import proyecto.servicios.interfaces.AutenticacionServicio;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacionController {

    private final AutenticacionServicio autenticacionServicio;
    private final AdministradorServicio administradorServicio;

    @PostMapping("/login")
    public ResponseEntity<MensajeDTO<TokenDTO>> login(@Valid @RequestBody LoginDTO loginDTO)
            throws Exception {
        TokenDTO tokenDTO = autenticacionServicio.login(loginDTO);
        return ResponseEntity.ok().body(new MensajeDTO<>(false, tokenDTO));
    }

    @GetMapping("/ciudades")
    public ResponseEntity<MensajeDTO<List<CiudadGetDTO>>>listarCiudades(){
        List<CiudadGetDTO> ciudadGetDTOS = autenticacionServicio.listarCiudades();
        return ResponseEntity.ok().body(new MensajeDTO<>(false, ciudadGetDTOS));
    }

}