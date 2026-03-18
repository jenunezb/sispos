package proyecto.controladores;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import proyecto.dto.*;
import proyecto.entidades.Administrador;
import proyecto.entidades.Empresa;
import proyecto.entidades.InformeInventarioDia;
import proyecto.entidades.Venta;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.EmpresaRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VentaRepository;
import proyecto.servicios.interfaces.AdministradorServicio;
import proyecto.servicios.interfaces.InformeInventarioDiaService;
import proyecto.servicios.interfaces.ProductoServicio;
import proyecto.servicios.interfaces.VendedorServicio;
import proyecto.servicios.interfaces.VentaServicio;
import proyecto.utils.JWTUtils;

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
    private final VentaServicio ventaServicio;
    private final JWTUtils jwtUtils;
    private final AdministradorRepository administradorRepository;
    private final EmpresaRepository empresaRepository;
    private final SedeRepository sedeRepository;
    private final VentaRepository ventaRepository;

    @PostMapping("/agregarVendedor")
    public ResponseEntity<MensajeDTO> crearVendedor(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit,
            @RequestBody UsuarioDTO dto) throws Exception {

        administradorServicio.crearVendedor(dto, resolverEmpresaNit(authorization, empresaNit));

        return ResponseEntity.ok(
                new MensajeDTO(false, "Vendedor creado exitosamente")
        );
    }

    @PostMapping("/crearProducto")
    public ResponseEntity<ProductoDTO> crearProducto(@Valid @RequestBody ProductoCrearDTO dto) {
        ProductoDTO productoCreado = productoService.crearProducto(dto);
        return ResponseEntity.ok(productoCreado);
    }

    @PutMapping("/productos/{codigo}/desactivar")
    public ResponseEntity<MensajeDTO> desactivarProducto(@PathVariable Long codigo) {

        productoService.desactivarProducto(codigo);

        return ResponseEntity.ok(
                new MensajeDTO(false, "Producto desactivado correctamente")
        );
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

    @GetMapping("/productos/{codigo}")
    public ResponseEntity<ProductoDTO> obtenerProducto(@PathVariable Long codigo) {

        ProductoDTO producto = productoService.obtenerProductoPorCodigo(codigo);
        return ResponseEntity.ok(producto);
    }

    @PutMapping("/productos")
    public ResponseEntity<ProductoDTO> editarProducto(@Valid @RequestBody ProductoActualizarDTO dto) {

        ProductoDTO productoActualizado = productoService.actualizarProducto(dto);
        return ResponseEntity.ok(productoActualizado);
    }

    @GetMapping("/listar-vendedores")
    public ResponseEntity<MensajeDTO<List<VendedorDTO>>> listarVendedores(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit
    ) {
        List<VendedorDTO> vendedores = vendedorServicio.listarVendedores(
                resolverEmpresaNit(authorization, empresaNit)
        );

        return ResponseEntity.ok(
                new MensajeDTO<>(false, vendedores)
        );
    }

    @PostMapping("/productos/importar-csv")
    public ResponseEntity<MensajeDTO<String>> importarProductosCsv(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit,
            @RequestParam("file") MultipartFile archivo) {

        int total = productoService.importarProductosCsv(
                archivo,
                resolverEmpresaNit(authorization, empresaNit)
        );

        return ResponseEntity.ok(
                new MensajeDTO<>(false, "Productos importados correctamente: " + total)
        );
    }

    @GetMapping("/listar-productos")
    public ResponseEntity<MensajeDTO<List<ProductoDTO>>> listarProductos(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit
    ) {

        List<ProductoDTO> productoDTOS = productoService.listarProductos(
                resolverEmpresaNit(authorization, empresaNit)
        );

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
    public ResponseEntity<MensajeDTO<String>> cambiarPassword(@PathVariable String correo, @RequestBody CambioPasswordDTO dto) throws Exception {

        administradorServicio.cambiarPassword(correo, dto.passwordActual(), dto.passwordNueva());

        return ResponseEntity.ok(
                new MensajeDTO<>(false, "Contraseña actualizada correctamente")
        );
    }

    @PatchMapping("/ventas/estado")
    public ResponseEntity<MensajeDTO> cambiarEstadoVenta(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody CambiarEstadoVentaDTO dto
    ) {
        Administrador admin = obtenerAdministradorAutenticado(authorization);

        if (admin.isEsSuperAdmin()) {
            ventaServicio.cambiarEstadoVentaSistema(dto.ventaId(), dto.valido());
        } else {
            ventaServicio.cambiarEstadoVenta(dto.ventaId(), dto.valido(), admin.getEmpresa().getNit());
        }

        String msg = dto.valido()
                ? "La venta fue marcada como válida"
                : "La venta fue marcada como inválida";

        return ResponseEntity.ok(new MensajeDTO(false, msg));
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

    @GetMapping("/sistema/empresas")
    public ResponseEntity<MensajeDTO<List<EmpresaResumenDTO>>> listarEmpresasSistema(
            @RequestHeader("Authorization") String authorization
    ) {
        validarAdministradorSistema(authorization);

        List<EmpresaResumenDTO> empresas = empresaRepository.findAll().stream()
                .map(empresa -> new EmpresaResumenDTO(
                        empresa.getNit(),
                        empresa.getNombre(),
                        empresa.getSedes() != null ? empresa.getSedes().size() : 0,
                        empresa.getAdministradores() != null
                                ? (int) empresa.getAdministradores().stream().filter(a -> !a.isEsSuperAdmin()).count()
                                : 0
                ))
                .toList();

        return ResponseEntity.ok(new MensajeDTO<>(false, empresas));
    }

    @GetMapping("/sistema/sedes")
    public ResponseEntity<MensajeDTO<List<SedeResumenDTO>>> listarSedesSistema(
            @RequestHeader("Authorization") String authorization
    ) {
        validarAdministradorSistema(authorization);

        List<SedeResumenDTO> sedes = sedeRepository.findAll().stream()
                .map(sede -> new SedeResumenDTO(
                        sede.getId(),
                        sede.getUbicacion(),
                        sede.getEmpresa() != null ? sede.getEmpresa().getNit() : null,
                        sede.getEmpresa() != null ? sede.getEmpresa().getNombre() : null,
                        sede.getAdministrador() != null ? sede.getAdministrador().getCorreo() : null
                ))
                .toList();

        return ResponseEntity.ok(new MensajeDTO<>(false, sedes));
    }

    @GetMapping("/sistema/administradores")
    public ResponseEntity<MensajeDTO<List<AdministradorResumenDTO>>> listarAdministradoresSistema(
            @RequestHeader("Authorization") String authorization
    ) {
        validarAdministradorSistema(authorization);

        List<AdministradorResumenDTO> administradores = administradorRepository.findAll().stream()
                .map(admin -> new AdministradorResumenDTO(
                        admin.getCodigo(),
                        admin.getNombre(),
                        admin.getApellido(),
                        admin.getCorreo(),
                        admin.getCelular(),
                        admin.getEmpresa() != null ? admin.getEmpresa().getNit() : null,
                        admin.getEmpresa() != null ? admin.getEmpresa().getNombre() : null,
                        admin.isEsSuperAdmin()
                ))
                .toList();

        return ResponseEntity.ok(new MensajeDTO<>(false, administradores));
    }

    @GetMapping("/sistema/ventas")
    public ResponseEntity<MensajeDTO<List<VentaSeguimientoDTO>>> listarVentasSistema(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit,
            @RequestParam(required = false) Long sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        validarAdministradorSistema(authorization);

        LocalDate fechaFin = hasta != null ? hasta : desde;

        List<VentaSeguimientoDTO> ventas = ventaRepository.buscarVentasSistema(
                        empresaNit,
                        sedeId,
                        desde != null ? desde.atStartOfDay() : null,
                        fechaFin != null ? fechaFin.atTime(23, 59, 59) : null
                ).stream()
                .map(this::mapToVentaSeguimiento)
                .toList();

        return ResponseEntity.ok(new MensajeDTO<>(false, ventas));
    }

    private VentaSeguimientoDTO mapToVentaSeguimiento(Venta venta) {
        String usuarioNombre = venta.getVendedor() != null
                ? venta.getVendedor().getNombre()
                : venta.getAdministrador() != null ? venta.getAdministrador().getNombre() : null;

        String usuarioCorreo = venta.getVendedor() != null
                ? venta.getVendedor().getCorreo()
                : venta.getAdministrador() != null ? venta.getAdministrador().getCorreo() : null;

        return new VentaSeguimientoDTO(
                venta.getId(),
                venta.getFecha(),
                venta.getTotal(),
                venta.getAnulado(),
                venta.getSede() != null && venta.getSede().getEmpresa() != null ? venta.getSede().getEmpresa().getNit() : null,
                venta.getSede() != null && venta.getSede().getEmpresa() != null ? venta.getSede().getEmpresa().getNombre() : null,
                venta.getSede() != null ? venta.getSede().getId() : null,
                venta.getSede() != null ? venta.getSede().getUbicacion() : null,
                usuarioNombre,
                usuarioCorreo,
                venta.getCliente() != null ? venta.getCliente().getId() : null,
                venta.getCliente() != null ? venta.getCliente().getNombre() : null,
                venta.getDetalles().stream()
                        .map(d -> new DetalleVentaResponseDTO(
                                d.getProducto() != null ? d.getProducto().getCodigo() : null,
                                d.getProducto() != null ? d.getProducto().getNombre() : d.getNombreLibre(),
                                d.getCantidad(),
                                d.getPrecioUnitario(),
                                d.getSubtotal(),
                                d.getNombreLibre()
                        ))
                        .toList()
        );
    }

    private Long resolverEmpresaNit(String authorization, Long empresaNitSolicitada) {
        Administrador admin = obtenerAdministradorAutenticado(authorization);

        if (admin.isEsSuperAdmin()) {
            if (empresaNitSolicitada == null) {
                throw new RuntimeException("Debe indicar el parámetro empresaNit para esta operación");
            }

            Empresa empresa = empresaRepository.findById(empresaNitSolicitada)
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
            return empresa.getNit();
        }

        if (admin.getEmpresa() == null) {
            throw new RuntimeException("El administrador no tiene una empresa asociada");
        }

        return admin.getEmpresa().getNit();
    }

    private void validarAdministradorSistema(String authorization) {
        Administrador admin = obtenerAdministradorAutenticado(authorization);
        if (!admin.isEsSuperAdmin()) {
            throw new RuntimeException("No tiene permisos de administrador del sistema");
        }
    }

    private Administrador obtenerAdministradorAutenticado(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        String correo = claims.getBody().getSubject();

        return administradorRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));
    }
}
