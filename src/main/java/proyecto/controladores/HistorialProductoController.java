package proyecto.controladores;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import proyecto.servicios.implementacion.*;
import java.time.LocalDate;
import java.util.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/inventarios/historial-productos")
public class HistorialProductoController {
    private final AdministradorAccesoService acceso;
    private final HistorialProductoService historial;
    private void validar(String authorization,long sede) {
        acceso.validarAccesoASede(acceso.obtenerAdministradorAutenticado(authorization),sede);
    }
    @GetMapping("/{sedeId}")
    public List<Map<String,Object>> listar(@RequestHeader("Authorization") String authorization,@PathVariable long sedeId) {
        validar(authorization,sedeId); return historial.listar(sedeId);
    }
    @GetMapping("/{sedeId}/{productoId}")
    public HistorialProductoService.ReporteProducto consultar(@RequestHeader("Authorization") String authorization,
            @PathVariable long sedeId,@PathVariable long productoId,@RequestParam LocalDate desde,@RequestParam LocalDate hasta) {
        validar(authorization,sedeId); return historial.consultar(sedeId,productoId,desde,hasta);
    }
}
