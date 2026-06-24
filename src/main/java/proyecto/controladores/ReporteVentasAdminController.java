package proyecto.controladores;

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
import proyecto.repositorios.SedeRepository;
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.servicios.interfaces.ReporteVentasAdminServicio;

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

    @GetMapping("/por-mes")
    public MensajeDTO<List<VentasPorMesDTO>> ventasPorMes(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit,
            @RequestParam(required = false) Long sedeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        return new MensajeDTO<>(false, reporteVentasAdminServicio.obtenerVentasPorMes(
                resolverSedeIds(admin, empresaNit, sedeId),
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
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        return new MensajeDTO<>(false, reporteVentasAdminServicio.obtenerVentasPorDia(
                resolverSedeIds(admin, empresaNit, sedeId),
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
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        return new MensajeDTO<>(false, reporteVentasAdminServicio.obtenerVentasPorHora(
                resolverSedeIds(admin, empresaNit, sedeId),
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
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        return new MensajeDTO<>(false, reporteVentasAdminServicio.obtenerComparativoMensual(
                resolverSedeIds(admin, empresaNit, sedeId),
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
        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        return new MensajeDTO<>(false, reporteVentasAdminServicio.obtenerCrecimientoMensual(
                resolverSedeIds(admin, empresaNit, sedeId),
                desde,
                hasta
        ));
    }

    private List<Long> resolverSedeIds(Administrador admin, Long empresaNit, Long sedeId) {
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
}
