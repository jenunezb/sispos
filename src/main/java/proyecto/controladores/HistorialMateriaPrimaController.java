package proyecto.controladores;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.servicios.implementacion.HistorialMateriaPrimaService;
import java.time.LocalDate;
import java.util.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/materias-primas/historial")
public class HistorialMateriaPrimaController {
    private final AdministradorAccesoService acceso;
    private final HistorialMateriaPrimaService historial;

    private void validar(String authorization, long sede) {
        acceso.validarAccesoASede(acceso.obtenerAdministradorAutenticado(authorization), sede);
    }

    @GetMapping("/{sedeId}")
    public List<Map<String, Object>> listar(@RequestHeader("Authorization") String authorization,
                                          @PathVariable long sedeId) {
        validar(authorization, sedeId);
        return historial.listar(sedeId);
    }

    @GetMapping("/{sedeId}/{materiaId}")
    public HistorialMateriaPrimaService.Reporte consultar(@RequestHeader("Authorization") String authorization,
            @PathVariable long sedeId, @PathVariable long materiaId,
            @RequestParam LocalDate desde, @RequestParam LocalDate hasta) {
        validar(authorization, sedeId);
        return historial.consultar(sedeId, materiaId, desde, hasta);
    }
}
