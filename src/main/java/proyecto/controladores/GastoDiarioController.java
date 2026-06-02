package proyecto.controladores;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.GastoDiarioCrearDTO;
import proyecto.dto.GastoDiarioResponseDTO;
import proyecto.dto.MensajeDTO;
import proyecto.entidades.Administrador;
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.servicios.implementacion.SuscripcionFeatureService;
import proyecto.servicios.interfaces.GastoDiarioServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/administrador/gastos")
@RequiredArgsConstructor
@CrossOrigin
public class GastoDiarioController {

    private final GastoDiarioServicio gastoDiarioServicio;
    private final AdministradorAccesoService administradorAccesoService;
    private final SuscripcionFeatureService suscripcionFeatureService;

    @PostMapping
    public ResponseEntity<MensajeDTO<GastoDiarioResponseDTO>> crear(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody GastoDiarioCrearDTO dto
    ) {
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        administradorAccesoService.validarAccesoASede(admin, dto.sedeId());
        suscripcionFeatureService.validarGastosHabilitados(dto.sedeId());

        return ResponseEntity.ok(new MensajeDTO<>(
                false,
                gastoDiarioServicio.crear(admin, dto)
        ));
    }

    @GetMapping
    public ResponseEntity<MensajeDTO<List<GastoDiarioResponseDTO>>> listar(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta
    ) {
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        Long empresaNitConsulta = administradorAccesoService.resolverEmpresaNit(admin, empresaNit);

        if (sedeId != null) {
            administradorAccesoService.validarAccesoASede(admin, sedeId);
            suscripcionFeatureService.validarGastosHabilitados(sedeId);
        }

        LocalDateTime fechaDesde = desde != null
                ? LocalDate.parse(desde).atStartOfDay()
                : LocalDate.now().atStartOfDay();
        LocalDateTime fechaHasta = hasta != null
                ? LocalDate.parse(hasta).atTime(23, 59, 59)
                : LocalDate.now().atTime(23, 59, 59);

        List<GastoDiarioResponseDTO> gastos = gastoDiarioServicio.listar(empresaNitConsulta, sedeId, fechaDesde, fechaHasta);
        var sedeIdsConGastos = suscripcionFeatureService.obtenerSedeIdsConGastosHabilitados(empresaNitConsulta);
        gastos = gastos.stream()
                .filter(gasto -> sedeIdsConGastos.contains(gasto.sedeId()))
                .toList();

        if (!admin.isEsSuperAdmin() && !admin.isEsAdministradorEmpresa()) {
            List<Long> sedeIdsVisibles = administradorAccesoService.obtenerSedesVisibles(admin).stream()
                    .map(sede -> sede.getId())
                    .toList();
            gastos = gastos.stream()
                    .filter(gasto -> sedeIdsVisibles.contains(gasto.sedeId()))
                    .toList();
        }

        return ResponseEntity.ok(new MensajeDTO<>(false, gastos));
    }
}
