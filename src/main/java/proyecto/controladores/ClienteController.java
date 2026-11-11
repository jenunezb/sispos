package proyecto.controladores;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.servicios.implementacion.ClienteServicio;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final ClienteServicio clienteServicio;

    @GetMapping
    public MensajeDTO<List<ClienteDTO>> listar(@RequestHeader("Authorization") String authorization) {
        return new MensajeDTO<>(false, clienteServicio.listar(authorization));
    }

    @PostMapping
    public MensajeDTO<ClienteDTO> crear(@RequestHeader("Authorization") String authorization,
                                      @Valid @RequestBody ClienteCrearDTO dto) {
        return new MensajeDTO<>(false, clienteServicio.crear(authorization, dto));
    }

    @PutMapping("/{id}")
    public MensajeDTO<ClienteDTO> actualizar(@RequestHeader("Authorization") String authorization,
                                           @PathVariable Long id,
                                           @Valid @RequestBody ClienteActualizarDTO dto) {
        return new MensajeDTO<>(false, clienteServicio.actualizar(authorization, id, dto));
    }
}
