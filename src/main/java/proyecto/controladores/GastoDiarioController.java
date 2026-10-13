package proyecto.controladores;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.GastoDiarioCrearDTO;
import proyecto.dto.GastoDiarioResponseDTO;
import proyecto.dto.MensajeDTO;
import proyecto.entidades.Administrador;
import proyecto.entidades.Vendedor;
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.servicios.implementacion.SuscripcionFeatureService;
import proyecto.servicios.interfaces.VendedorServicio;
import proyecto.servicios.interfaces.GastoDiarioServicio;
import proyecto.utils.FechaColombiaUtils;
import proyecto.utils.JWTUtils;

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
    private final VendedorServicio vendedorServicio;
    private final JWTUtils jwtUtils;

    @PostMapping
    public ResponseEntity<MensajeDTO<GastoDiarioResponseDTO>> crear(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody GastoDiarioCrearDTO dto
    ) {
        String rol = obtenerRol(authorization);
        suscripcionFeatureService.validarGastosHabilitados(dto.sedeId());

        if ("vendedor".equals(rol)) {
            Vendedor vendedor = obtenerVendedorAutenticado(authorization);
            validarAccesoSedeVendedor(vendedor, dto.sedeId());

            return ResponseEntity.ok(new MensajeDTO<>(
                    false,
                    gastoDiarioServicio.crear(vendedor, dto)
            ));
        }

        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        administradorAccesoService.validarAccesoASede(admin, dto.sedeId());

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
        String rol = obtenerRol(authorization);

        LocalDateTime fechaDesde = desde != null
                ? java.time.LocalDate.parse(desde).atStartOfDay()
                : FechaColombiaUtils.hoy().atStartOfDay();
        LocalDateTime fechaHasta = hasta != null
                ? java.time.LocalDate.parse(hasta).atTime(23, 59, 59)
                : FechaColombiaUtils.hoy().atTime(23, 59, 59);

        if ("vendedor".equals(rol)) {
            Vendedor vendedor = obtenerVendedorAutenticado(authorization);
            Long sedeIdVendedor = vendedor.getSede() != null ? vendedor.getSede().getId() : null;
            if (sedeIdVendedor == null) {
                throw new RuntimeException("El vendedor no tiene una sede asociada");
            }
            if (sedeId != null && !sedeIdVendedor.equals(sedeId)) {
                throw new RuntimeException("No tiene permisos para consultar gastos de otra sede");
            }

            suscripcionFeatureService.validarGastosHabilitados(sedeIdVendedor);
            Long empresaNitConsulta = obtenerEmpresaNitVendedor(vendedor);
            List<GastoDiarioResponseDTO> gastos = gastoDiarioServicio.listar(empresaNitConsulta, sedeIdVendedor, fechaDesde, fechaHasta);
            return ResponseEntity.ok(new MensajeDTO<>(false, gastos));
        }

        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        Long empresaNitConsulta = administradorAccesoService.resolverEmpresaNit(admin, empresaNit);

        if (sedeId != null) {
            administradorAccesoService.validarAccesoASede(admin, sedeId);
            suscripcionFeatureService.validarGastosHabilitados(sedeId);
        }

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
