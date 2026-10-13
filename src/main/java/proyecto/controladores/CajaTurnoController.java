package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
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
import proyecto.entidades.Vendedor;
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.servicios.implementacion.SuscripcionFeatureService;
import proyecto.servicios.interfaces.CajaTurnoServicio;
import proyecto.servicios.interfaces.VendedorServicio;
import proyecto.utils.JWTUtils;

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
    private final VendedorServicio vendedorServicio;
    private final SuscripcionFeatureService suscripcionFeatureService;
    private final JWTUtils jwtUtils;

    @PostMapping("/aperturas")
    public ResponseEntity<MensajeDTO<CajaTurnoResponseDTO>> abrirCaja(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CajaAperturaDTO dto
    ) {
        String rol = obtenerRol(authorization);
        suscripcionFeatureService.validarCajaHabilitada(dto.sedeId());

        if ("vendedor".equals(rol)) {
            Vendedor vendedor = obtenerVendedorAutenticado(authorization);
            validarAccesoSedeVendedor(vendedor, dto.sedeId());

            return ResponseEntity.ok(new MensajeDTO<>(
                    false,
                    cajaTurnoServicio.abrirCaja(vendedor, dto)
            ));
        }

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
        String rol = obtenerRol(authorization);
        CajaTurnoResponseDTO cajaActual = cajaTurnoServicio.obtenerPorId(cajaId);
        suscripcionFeatureService.validarCajaHabilitada(cajaActual.sedeId());

        if ("vendedor".equals(rol)) {
            Vendedor vendedor = obtenerVendedorAutenticado(authorization);
            validarAccesoSedeVendedor(vendedor, cajaActual.sedeId());

            return ResponseEntity.ok(new MensajeDTO<>(
                    false,
                    cajaTurnoServicio.cerrarCaja(vendedor, cajaId, dto)
            ));
        }

        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
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
        String rol = obtenerRol(authorization);
        suscripcionFeatureService.validarCajaHabilitada(sedeId);

        if ("vendedor".equals(rol)) {
            Vendedor vendedor = obtenerVendedorAutenticado(authorization);
            validarAccesoSedeVendedor(vendedor, sedeId);

            return ResponseEntity.ok(new MensajeDTO<>(
                    false,
                    cajaTurnoServicio.obtenerCajaActual(sedeId)
            ));
        }

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
        String rol = obtenerRol(authorization);

        LocalDateTime fechaDesde = desde != null
                ? LocalDate.parse(desde).atStartOfDay()
                : LocalDate.now().atStartOfDay();
        LocalDateTime fechaHasta = hasta != null
                ? LocalDate.parse(hasta).atTime(23, 59, 59)
                : LocalDate.now().atTime(23, 59, 59);

        if ("vendedor".equals(rol)) {
            Vendedor vendedor = obtenerVendedorAutenticado(authorization);
            Long sedeIdVendedor = vendedor.getSede() != null ? vendedor.getSede().getId() : null;
            if (sedeIdVendedor == null) {
                throw new RuntimeException("El vendedor no tiene una sede asociada");
            }
            if (sedeId != null && !sedeIdVendedor.equals(sedeId)) {
                throw new RuntimeException("No tiene permisos para consultar cajas de otra sede");
            }

            suscripcionFeatureService.validarCajaHabilitada(sedeIdVendedor);
            List<CajaTurnoResponseDTO> cajas = cajaTurnoServicio.listar(
                    obtenerEmpresaNitVendedor(vendedor),
                    sedeIdVendedor,
                    fechaDesde,
                    fechaHasta
            );
            return ResponseEntity.ok(new MensajeDTO<>(false, cajas));
        }

        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        Long empresaNitConsulta = administradorAccesoService.resolverEmpresaNit(admin, empresaNit);

        if (sedeId != null) {
            administradorAccesoService.validarAccesoASede(admin, sedeId);
        }

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

    private String obtenerRol(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        return String.valueOf(claims.getBody().get("rol"));
    }

    private Vendedor obtenerVendedorAutenticado(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        return vendedorServicio.obtenerVendedorPorCorreo(claims.getBody().getSubject());
    }

    private void validarAccesoSedeVendedor(Vendedor vendedor, Long sedeId) {
        Long sedeIdVendedor = vendedor.getSede() != null ? vendedor.getSede().getId() : null;
        if (sedeIdVendedor == null || !sedeIdVendedor.equals(sedeId)) {
            throw new RuntimeException("No tiene permisos para acceder a la sede seleccionada");
        }
    }

    private Long obtenerEmpresaNitVendedor(Vendedor vendedor) {
        if (vendedor.getEmpresa() != null && vendedor.getEmpresa().getNit() != null) {
            return vendedor.getEmpresa().getNit();
        }
        if (vendedor.getSede() != null && vendedor.getSede().getEmpresa() != null) {
            return vendedor.getSede().getEmpresa().getNit();
        }
        throw new RuntimeException("El vendedor no tiene una empresa asociada");
    }
}
