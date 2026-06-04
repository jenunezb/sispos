package proyecto.controladores;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.CajaAperturaDTO;
import proyecto.dto.CajaCierreDTO;
import proyecto.dto.CajaTurnoResponseDTO;
import proyecto.dto.MensajeDTO;
import proyecto.entidades.Administrador;
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.servicios.interfaces.CajaTurnoServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/administrador/cajas")
@RequiredArgsConstructor
@CrossOrigin
public class CajaTurnoController {

    private final CajaTurnoServicio cajaTurnoServicio;
    private final AdministradorAccesoService administradorAccesoService;

    @PostMapping("/aperturas")
    public ResponseEntity<MensajeDTO<CajaTurnoResponseDTO>> abrirCaja(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CajaAperturaDTO dto
    ) {
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        administradorAccesoService.validarAccesoASede(admin, dto.sedeId());

        return ResponseEntity.ok(new MensajeDTO<>(
                false,
                cajaTurnoServicio.abrirCaja(admin, dto)
        ));
    }

    @PostMapping("/{cajaId}/cierres")
    public ResponseEntity<MensajeDTO<CajaTurnoResponseDTO>> cerrarCaja(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long cajaId,
            @Valid @RequestBody CajaCierreDTO dto
    ) {
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        CajaTurnoResponseDTO cajaActual = cajaTurnoServicio.obtenerPorId(cajaId);
        administradorAccesoService.validarAccesoASede(admin, cajaActual.sedeId());

        return ResponseEntity.ok(new MensajeDTO<>(
                false,
                cajaTurnoServicio.cerrarCaja(admin, cajaId, dto)
        ));
    }

    @GetMapping("/actual")
    public ResponseEntity<MensajeDTO<CajaTurnoResponseDTO>> obtenerCajaActual(
            @RequestHeader("Authorization") String authorization,
            @RequestParam Long sedeId
    ) {
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        administradorAccesoService.validarAccesoASede(admin, sedeId);

        return ResponseEntity.ok(new MensajeDTO<>(
                false,
                cajaTurnoServicio.obtenerCajaActual(sedeId)
        ));
    }

    @GetMapping
    public ResponseEntity<MensajeDTO<List<CajaTurnoResponseDTO>>> listar(
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
        }

        LocalDateTime fechaDesde = desde != null
                ? LocalDate.parse(desde).atStartOfDay()
                : LocalDate.now().atStartOfDay();
        LocalDateTime fechaHasta = hasta != null
                ? LocalDate.parse(hasta).atTime(23, 59, 59)
                : LocalDate.now().atTime(23, 59, 59);

        List<CajaTurnoResponseDTO> cajas = cajaTurnoServicio.listar(empresaNitConsulta, sedeId, fechaDesde, fechaHasta);

        if (!admin.isEsSuperAdmin() && !admin.isEsAdministradorEmpresa()) {
            List<Long> sedeIdsVisibles = administradorAccesoService.obtenerSedesVisibles(admin).stream()
                    .map(sede -> sede.getId())
                    .toList();
            cajas = cajas.stream()
                    .filter(caja -> sedeIdsVisibles.contains(caja.sedeId()))
                    .toList();
        }

        return ResponseEntity.ok(new MensajeDTO<>(false, cajas));
    }
}
