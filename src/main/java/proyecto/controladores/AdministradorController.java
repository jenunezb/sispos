package proyecto.controladores;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.servicios.interfaces.AdministradorServicio;
import proyecto.servicios.interfaces.ProductoServicio;
import proyecto.servicios.interfaces.VendedorServicio;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("api/administrador")
@AllArgsConstructor
public class AdministradorController {

    private final AdministradorServicio administradorServicio;
    private final ProductoServicio productoService;
    private final VendedorServicio vendedorServicio;

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
    public ResponseEntity<MensajeDTO> desactivarProducto(
            @PathVariable Long codigo) {
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
}