package proyecto.controladores;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.CorregirPagoDTO;
import proyecto.dto.VentaResponseDTO;
import proyecto.entidades.CorreccionPagoVenta;
import proyecto.servicios.implementacion.CorreccionPagoService;
import java.util.List;

@RestController @RequiredArgsConstructor
@RequestMapping("api/ventas/{ventaId}/pago")
public class CorreccionPagoController {
    private final CorreccionPagoService service;

    @PatchMapping
    public VentaResponseDTO corregir(@RequestHeader("Authorization") String authorization,
            @PathVariable Long ventaId, @Valid @RequestBody CorregirPagoDTO dto) {
        return service.corregir(authorization, ventaId, dto);
    }

    @GetMapping("/historial")
    public List<CorreccionPagoVenta> historial(@RequestHeader("Authorization") String authorization,
            @PathVariable Long ventaId) {
        return service.historial(authorization, ventaId);
    }
}
