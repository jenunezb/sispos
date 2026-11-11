package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.entidades.Sede;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.*;
import proyecto.utils.JWTUtils;
import proyecto.dto.VentaResponseDTO;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VentaAccesoService {
    private final ClienteServicio clienteServicio;
    private final AdministradorAccesoService administradorAccesoService;
    private final SedeRepository sedeRepository;
    private final VentaRepository ventaRepository;
    private final VendedorRepository vendedorRepository;
    private final JWTUtils jwtUtils;

    public void validarSede(String authorization, Long sedeId) {
        if (sedeId == null) {
            throw new IllegalArgumentException("Debe indicar una sede");
        }
        var empresa = clienteServicio.empresaAutenticada(authorization);
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada"));
        if (sede.getEmpresa() == null || !empresa.getNit().equals(sede.getEmpresa().getNit())) {
            throw new IllegalArgumentException("La sede no pertenece a su empresa");
        }
        var claims = jwtUtils.parseJwt(authorization.replace("Bearer ", "")).getBody();
        if ("administrador".equals(claims.get("rol", String.class))) {
            administradorAccesoService.validarAccesoAutenticadoASede(authorization, sedeId);
        } else {
            Vendedor vendedor = vendedorRepository.findByCorreo(claims.getSubject())
                    .orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado"));
            if (vendedor.getSede() == null || !sedeId.equals(vendedor.getSede().getId())) {
                throw new IllegalArgumentException("No tiene permisos para acceder a la sede seleccionada");
            }
        }
    }

    public void validarVenta(String authorization, Long ventaId) {
        var venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada"));
        validarSede(authorization, venta.getSede() != null ? venta.getSede().getId() : null);
    }

    public void validarVendedor(String authorization, Long vendedorId) {
        validarVendedor(authorization, vendedorRepository.findById(vendedorId)
                .orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado")));
    }

    public void validarCorreoVendedor(String authorization, String correo) {
        validarVendedor(authorization, vendedorRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado")));
    }

    public List<VentaResponseDTO> validarResultados(String authorization, List<VentaResponseDTO> resultados) {
        // Un vendedor trasladado puede conservar ventas historicas de otra sede o empresa.
        var ventas = ventaRepository.findAllById(resultados.stream().map(VentaResponseDTO::id).toList());
        if (ventas.size() != resultados.size()) {
            throw new IllegalArgumentException("No se pudo validar el acceso a las ventas");
        }
        ventas.stream().map(venta -> venta.getSede() != null ? venta.getSede().getId() : null)
                .distinct().forEach(sedeId -> validarSede(authorization, sedeId));
        return resultados;
    }

    private void validarVendedor(String authorization, Vendedor vendedor) {
        validarSede(authorization, vendedor.getSede() != null ? vendedor.getSede().getId() : null);
    }
}
