package proyecto.controladores;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.entidades.Administrador;
import proyecto.entidades.TokenValidacion;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.TokenValidacionRepository;
import proyecto.servicios.implementacion.EmailService;
import proyecto.servicios.interfaces.AdministradorServicio;
import proyecto.servicios.interfaces.AutenticacionServicio;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacionController {

    private final AutenticacionServicio autenticacionServicio;
    private final AdministradorServicio administradorServicio;
    private final AdministradorRepository administradorRepository;
    private final TokenValidacionRepository tokenValidacionRepository;
    private final EmailService emailService;


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

    @PostMapping("/crearAdministrador")
    public ResponseEntity<MensajeDTO<String>> crearAdministrador(@Valid @RequestBody AdministradorDTO administradorDTO)throws Exception{
        administradorServicio.crearAdministrador(administradorDTO);
        return ResponseEntity.ok().body(new MensajeDTO<>(false, "se agregó el administrador correctamente"));
    }

    @GetMapping("/confirmar")
    public ResponseEntity<String> confirmarCorreo(@RequestParam String token) {

        TokenValidacion tokenVal = tokenValidacionRepository
                .findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (tokenVal.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        Administrador admin = tokenVal.getAdministrador();
        admin.setActivo(true);
        administradorRepository.save(admin);

        tokenValidacionRepository.delete(tokenVal);

        return ResponseEntity.ok("Correo validado correctamente. Ya puedes iniciar sesión.");
    }

    @GetMapping("/test-mail")
    public String testMail() {
        emailService.enviarCorreo(
                "prhoteuz@gmail.com",
                "Prueba correo",
                "Si ves esto, el SMTP funciona 🚀"
        );
        return "Correo enviado";
    }

}