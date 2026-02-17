package proyecto.controladores;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    @PostMapping(value = "/registro", consumes = "multipart/form-data")
    public ResponseEntity<MensajeDTO<String>> registro(
            @RequestPart("datos") String datosJson,
            @RequestPart("logo") MultipartFile archivo
    ) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        RegistroEmpresaDTO dto =
                objectMapper.readValue(datosJson, RegistroEmpresaDTO.class);

        administradorServicio.registrarEmpresa(dto, archivo);

        return ResponseEntity.ok()
                .body(new MensajeDTO<>(false, "Empresa y administrador creados correctamente"));
    }

}