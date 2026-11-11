package proyecto.controladores;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.servicios.implementacion.ResumenSeguimientoService;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/inventarios/seguimiento")
public class ResumenSeguimientoController {
    private final AdministradorAccesoService acceso;
    private final ResumenSeguimientoService resumen;
    private final proyecto.servicios.implementacion.SuscripcionFeatureService suscripcionFeatureService;
    @GetMapping("/{sedeId}/hoy")
    public ResumenSeguimientoService.Resumen hoy(@RequestHeader("Authorization") String authorization,@PathVariable long sedeId) {
        acceso.validarAccesoASede(acceso.obtenerAdministradorAutenticado(authorization),sedeId);
        suscripcionFeatureService.validarInformesHabilitados(sedeId);
        return resumen.hoy(sedeId);
    }
}
