package proyecto.controladores;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.MensajeDTO;
import proyecto.dto.SedeActualizarDTO;
import proyecto.dto.SedeCrearDTO;
import proyecto.dto.SedeDTO;
import proyecto.entidades.Administrador;
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.servicios.interfaces.SedeServicio;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/sedes")
@AllArgsConstructor
public class SedeController {
    private final SedeServicio sedeServicio;
    private final AdministradorAccesoService administradorAccesoService;

    @PostMapping
    public ResponseEntity<SedeDTO> crear(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody SedeCrearDTO dto
    ) {
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        administradorAccesoService.validarAdministradorEmpresa(admin);
        return ResponseEntity.ok(sedeServicio.crear(dto, admin.getEmpresa().getNit()));
    }

    @GetMapping
    public ResponseEntity<List<SedeDTO>> listar(@RequestHeader("Authorization") String authorization) {
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        return ResponseEntity.ok(sedeServicio.listar(administradorAccesoService.obtenerSedesVisibles(admin)));
    }

    @PutMapping
    public ResponseEntity<SedeDTO> actualizar(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody SedeActualizarDTO dto
    ) {
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        administradorAccesoService.validarAdministradorEmpresa(admin);
        administradorAccesoService.validarAccesoASede(admin, dto.id());
        return ResponseEntity.ok(sedeServicio.actualizar(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MensajeDTO<SedeDTO>> obtenerPorId(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id
    ) {
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        administradorAccesoService.validarAccesoASede(admin, id);
        return ResponseEntity.ok(
                new MensajeDTO<>(false, sedeServicio.obtenerPorId(id))
        );
    }
}
