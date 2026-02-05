package proyecto.controladores;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.entidades.InformeInventarioDia;
import proyecto.servicios.interfaces.AdministradorServicio;
import proyecto.servicios.interfaces.InformeInventarioDiaService;
import proyecto.servicios.interfaces.ProductoServicio;
import proyecto.servicios.interfaces.VendedorServicio;

import java.time.LocalDate;
import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/administrador")
@AllArgsConstructor
public class AdministradorController {

    private final AdministradorServicio administradorServicio;
    private final ProductoServicio productoService;
    private final VendedorServicio vendedorServicio;
    private final InformeInventarioDiaService informeInventarioDiaService;


    @PostMapping("/agregarVendedor")
    public ResponseEntity<MensajeDTO> crearVendedor(@RequestBody UsuarioDTO dto) throws Exception {

        administradorServicio.crearVendedor(dto);

        return ResponseEntity.ok(
                new MensajeDTO(false, "Vendedor creado exitosamente")
        );
    }

    @PostMapping("/crearAdministrador")
    public ResponseEntity<MensajeDTO<String>> crearAdministrador(@Valid @RequestBody AdministradorDTO administradorDTO)throws Exception{
        administradorServicio.crearAdministrador(administradorDTO);
        return ResponseEntity.ok().body(new MensajeDTO<>(false, "se agregó el administrador correctamente"));
    }

    @PostMapping("/crearProducto")
    public ResponseEntity<ProductoDTO> crearProducto(@Valid @RequestBody ProductoCrearDTO dto) {
        ProductoDTO productoCreado = productoService.crearProducto(dto);
        return ResponseEntity.ok(productoCreado);
    }

    @PatchMapping("/estado")
    public ResponseEntity<MensajeDTO> cambiarEstado(@Valid @RequestBody CambiarEstadoVendedorDTO dto) {

        vendedorServicio.cambiarEstado(dto.codigo(), dto.estado());

        String texto = dto.estado()
                ? "El vendedor fue activado exitosamente"
                : "El vendedor fue desactivado exitosamente";

        return ResponseEntity.ok(
                new MensajeDTO(false, texto)
        );
    }

    /**
     * Buscar producto por código
     */
    @GetMapping("/productos/{codigo}")
    public ResponseEntity<ProductoDTO> obtenerProducto(@PathVariable Long codigo) {

        ProductoDTO producto = productoService.obtenerProductoPorCodigo(codigo);
        return ResponseEntity.ok(producto);
    }

    /**
     * Editar producto
     */
    @PutMapping("/productos")
    public ResponseEntity<ProductoDTO> editarProducto(@Valid @RequestBody ProductoActualizarDTO dto) {

        ProductoDTO productoActualizado = productoService.actualizarProducto(dto);
        return ResponseEntity.ok(productoActualizado);
    }

    /**
     * Eliminar producto
     */
    @PutMapping("/productos/{codigo}/desactivar")
    public ResponseEntity<MensajeDTO> desactivarProducto(@PathVariable Long codigo) {
        productoService.eliminarProducto(codigo);
        return ResponseEntity.ok(
                new MensajeDTO(false, "Producto desactivado correctamente")
        );
    }

    @GetMapping("/listar-vendedores")
    public ResponseEntity<MensajeDTO<List<VendedorDTO>>> listarVendedores() {

        List<VendedorDTO> vendedores = vendedorServicio.listarVendedores();

        return ResponseEntity.ok(
                new MensajeDTO<>(false, vendedores)
        );
    }

    @GetMapping("/listar-productos")
    public ResponseEntity<MensajeDTO<List<ProductoDTO>>> listarProductos() {

        List<ProductoDTO> productoDTOS = productoService.listarProductos();

        return ResponseEntity.ok(
                new MensajeDTO<>(false, productoDTOS)
        );
    }

    @PutMapping("/editarVendedor")
    public ResponseEntity<MensajeDTO> editarVendedor(@Valid @RequestBody UsuarioDTO dto) throws Exception {

        administradorServicio.editarVendedor(dto);

        return ResponseEntity.ok(
                new MensajeDTO(false, "Vendedor actualizado correctamente")
        );
    }

    @GetMapping("/final/{sedeId}")
    public ResponseEntity<List<InventarioFinalDTO>> obtenerInventarioFinal(@PathVariable Long sedeId,
                                                                           @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaInicio,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaFin
    ) {

        List<InventarioFinalDTO> resultado =
                administradorServicio.obtenerInventarioFinal(
                        sedeId, fechaInicio, fechaFin
                );

        return ResponseEntity.ok(resultado);
    }

    @PutMapping("/cuentas/{correo}")
    public ResponseEntity<MensajeDTO<String>> cambiarPassword( @PathVariable String correo, @RequestBody CambioPasswordDTO dto) throws Exception {

        administradorServicio.cambiarPassword( correo, dto.passwordActual(), dto.passwordNueva());

        return ResponseEntity.ok(
                new MensajeDTO<>(false, "Contraseña actualizada correctamente")
        );
    }

    @PostMapping("/guardar")
    public ResponseEntity<?> guardarInforme(@RequestBody InformeInventarioDiaDTO dto) {
        try {
            informeInventarioDiaService.guardarInforme(dto);
            return ResponseEntity.ok("Informe guardado correctamente");
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar el informe");
        }
    }

    @GetMapping("/informes")
    public ResponseEntity<List<InformeInventarioDia>> obtenerInformes(
            @RequestParam Long sedeId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha) {

        return ResponseEntity.ok(
                informeInventarioDiaService.obtenerInformes(sedeId, fecha)
        );
    }

}