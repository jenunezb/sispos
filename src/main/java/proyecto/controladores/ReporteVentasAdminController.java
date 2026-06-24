package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import proyecto.dto.ComparativoVentasMensualDTO;
import proyecto.dto.CrecimientoVentasMensualDTO;
import proyecto.dto.MensajeDTO;
import proyecto.dto.VentasPorDiaDTO;
import proyecto.dto.VentasPorHoraDTO;
import proyecto.dto.VentasPorMesDTO;
import proyecto.entidades.Administrador;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.SedeRepository;
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.servicios.interfaces.ReporteVentasAdminServicio;
import proyecto.servicios.interfaces.VendedorServicio;
import proyecto.utils.JWTUtils;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/administrador/reportes/ventas")
@RequiredArgsConstructor
@CrossOrigin
public class ReporteVentasAdminController {

    private final ReporteVentasAdminServicio reporteVentasAdminServicio;
    private final AdministradorAccesoService administradorAccesoService;
    private final SedeRepository sedeRepository;
    private final VendedorServicio vendedorServicio;
    private final JWTUtils jwtUtils;

    @GetMapping("/por-mes")
    public MensajeDTO<List<VentasPorMesDTO>> ventasPorMes(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit,
            @RequestParam(required = false) Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return new MensajeDTO<>(false, reporteVentasAdminServicio.obtenerVentasPorMes(
                resolverSedeIds(authorization, empresaNit, sedeId),
                desde,
                hasta
        ));
    }

    @GetMapping("/por-dia")
    public MensajeDTO<List<VentasPorDiaDTO>> ventasPorDia(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit,
            @RequestParam(required = false) Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return new MensajeDTO<>(false, reporteVentasAdminServicio.obtenerVentasPorDia(
                resolverSedeIds(authorization, empresaNit, sedeId),
                desde,
                hasta
        ));
    }

    @GetMapping("/por-hora")
    public MensajeDTO<List<VentasPorHoraDTO>> ventasPorHora(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit,
            @RequestParam(required = false) Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return new MensajeDTO<>(false, reporteVentasAdminServicio.obtenerVentasPorHora(
                resolverSedeIds(authorization, empresaNit, sedeId),
                desde,
                hasta
        ));
    }

    @GetMapping("/comparativo-mensual")
    public MensajeDTO<ComparativoVentasMensualDTO> comparativoMensual(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) Integer mes
    ) {
        return new MensajeDTO<>(false, reporteVentasAdminServicio.obtenerComparativoMensual(
                resolverSedeIds(authorization, empresaNit, sedeId),
                anio,
                mes
        ));
    }

    @GetMapping("/crecimiento-mensual")
    public MensajeDTO<List<CrecimientoVentasMensualDTO>> crecimientoMensual(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit,
            @RequestParam(required = false) Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return new MensajeDTO<>(false, reporteVentasAdminServicio.obtenerCrecimientoMensual(
                resolverSedeIds(authorization, empresaNit, sedeId),
                desde,
                hasta
        ));
    }

    private List<Long> resolverSedeIds(String authorization, Long empresaNit, Long sedeId) {
        String rol = obtenerRol(authorization);

        if ("vendedor".equals(rol)) {
            Vendedor vendedor = obtenerVendedorAutenticado(authorization);
            Long sedeIdVendedor = vendedor.getSede() != null ? vendedor.getSede().getId() : null;
            if (sedeIdVendedor == null) {
                throw new RuntimeException("El vendedor no tiene una sede asociada");
            }
            if (sedeId != null && !sedeIdVendedor.equals(sedeId)) {
                throw new RuntimeException("No tiene permisos para consultar reportes de otra sede");
            }
            return List.of(sedeIdVendedor);
        }

        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        if (sedeId != null) {
            administradorAccesoService.validarAccesoASede(admin, sedeId);
            return List.of(sedeId);
        }

        Long empresaNitConsulta = administradorAccesoService.resolverEmpresaNit(admin, empresaNit);
        List<Long> sedeIdsVisibles = administradorAccesoService.obtenerSedesVisibles(admin).stream()
                .map(sede -> sede.getId())
                .toList();

        return sedeRepository.findByEmpresaNitAndIdIn(empresaNitConsulta, sedeIdsVisibles).stream()
                .map(sede -> sede.getId())
                .toList();
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
}
