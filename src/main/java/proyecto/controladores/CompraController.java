package proyecto.controladores;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.servicios.implementacion.CompraService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController @RequiredArgsConstructor @RequestMapping("/api/compras") @CrossOrigin
public class CompraController {
    private final CompraService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> recibir(@RequestHeader("Authorization") String authorization, @Valid @RequestBody CompraDTO dto) {
        return service.recibir(authorization,dto);
    }
    @GetMapping
    public List<Map<String,Object>> listar(@RequestHeader("Authorization") String authorization,
        @RequestParam(required=false) Long sedeId,
        @RequestParam(required=false) Long empresaNit,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta) {
        if(desde!=null && hasta!=null && desde.isAfter(hasta)) throw new IllegalArgumentException("El rango de fechas es inválido");
        return service.listar(authorization,sedeId,empresaNit,desde,hasta);
    }
    @PostMapping("/{compraId}/pagos")
    public Map<String,Object> abonar(@RequestHeader("Authorization") String authorization,@PathVariable Long compraId,@Valid @RequestBody PagoCompraDTO dto) {
        return service.abonar(authorization,compraId,dto);
    }
    @GetMapping("/proveedores")
    public List<Map<String,Object>> proveedores(@RequestHeader("Authorization") String authorization,@RequestParam(required=false) Long empresaNit) {
        return service.listarProveedores(authorization,empresaNit);
    }
    @PostMapping("/proveedores")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> crearProveedor(@RequestHeader("Authorization") String authorization,@RequestParam(required=false) Long empresaNit,@Valid @RequestBody ProveedorDTO dto) {
        return service.crearProveedor(authorization,empresaNit,dto);
    }
}
