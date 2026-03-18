package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import proyecto.dto.MensajeDTO;
import proyecto.dto.SuperAdminDetalleVentaDTO;
import proyecto.dto.SuperAdminVentaDTO;
import proyecto.entidades.Administrador;
import proyecto.entidades.DetalleVenta;
import proyecto.entidades.Venta;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.VentaRepository;
import proyecto.utils.JWTUtils;

import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final VentaRepository ventaRepository;
    private final AdministradorRepository administradorRepository;
    private final JWTUtils jwtUtils;

    @GetMapping("/ventas")
    public ResponseEntity<MensajeDTO<List<SuperAdminVentaDTO>>> listarTodasLasVentas(
            @RequestHeader("Authorization") String authorization
    ) {
        validarSuperAdmin(authorization);

        List<SuperAdminVentaDTO> ventas = ventaRepository.findAllConDetalleParaSuperAdmin()
                .stream()
                .map(this::mapToDto)
                .toList();

        return ResponseEntity.ok(new MensajeDTO<>(false, ventas));
    }

    @GetMapping("/ventas/{ventaId}")
    public ResponseEntity<MensajeDTO<SuperAdminVentaDTO>> obtenerVentaPorId(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long ventaId
    ) {
        validarSuperAdmin(authorization);

        Venta venta = ventaRepository.findDetalleByIdParaSuperAdmin(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        return ResponseEntity.ok(new MensajeDTO<>(false, mapToDto(venta)));
    }

    private SuperAdminVentaDTO mapToDto(Venta venta) {
        String administradorNombre = venta.getAdministrador() != null
                ? (venta.getAdministrador().getNombre() + " " + venta.getAdministrador().getApellido()).trim()
                : null;

        return new SuperAdminVentaDTO(
                venta.getId(),
                venta.getFecha(),
                venta.getTotal(),
                venta.getAnulado(),
                !Boolean.TRUE.equals(venta.getAnulado()),
                venta.getModoPago() != null ? venta.getModoPago().name() : null,
                venta.getSede() != null && venta.getSede().getEmpresa() != null ? venta.getSede().getEmpresa().getNit() : null,
                venta.getSede() != null && venta.getSede().getEmpresa() != null ? venta.getSede().getEmpresa().getNombre() : null,
                venta.getSede() != null ? venta.getSede().getId() : null,
                venta.getSede() != null ? venta.getSede().getUbicacion() : null,
                venta.getVendedor() != null ? venta.getVendedor().getCodigo() : null,
                venta.getVendedor() != null ? venta.getVendedor().getNombre() : null,
                venta.getVendedor() != null ? venta.getVendedor().getCorreo() : null,
                venta.getAdministrador() != null ? venta.getAdministrador().getCodigo() : null,
                administradorNombre,
                venta.getAdministrador() != null ? venta.getAdministrador().getCorreo() : null,
                venta.getCliente() != null ? venta.getCliente().getId() : null,
                venta.getCliente() != null ? venta.getCliente().getNombre() : null,
                venta.getCliente() != null ? venta.getCliente().getTelefono() : null,
                venta.getCliente() != null ? venta.getCliente().getDocumento() : null,
                venta.getDetalles().stream().map(this::mapDetalle).toList()
        );
    }

    private SuperAdminDetalleVentaDTO mapDetalle(DetalleVenta detalle) {
        return new SuperAdminDetalleVentaDTO(
                detalle.getId(),
                detalle.getProducto() != null ? detalle.getProducto().getCodigo() : null,
                detalle.getProducto() != null ? detalle.getProducto().getNombre() : null,
                detalle.getNombreLibre(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal()
        );
    }

    private void validarSuperAdmin(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        String correo = claims.getBody().getSubject();
        String rol = (String) claims.getBody().get("rol");
        Boolean esSuperAdmin = claims.getBody().get("esSuperAdmin", Boolean.class);

        if (!"administrador".equals(rol) || !Boolean.TRUE.equals(esSuperAdmin)) {
            throw new RuntimeException("No tiene permisos de superadministrador");
        }

        Administrador administrador = administradorRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));

        if (!administrador.isEsSuperAdmin()) {
            throw new RuntimeException("No tiene permisos de superadministrador");
        }
    }
}
