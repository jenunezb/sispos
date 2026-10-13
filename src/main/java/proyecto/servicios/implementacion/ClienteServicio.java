package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import proyecto.dto.*;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.utils.JWTUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteServicio {
    private final ClienteRepository clienteRepository;
    private final AdministradorRepository administradorRepository;
    private final VendedorRepository vendedorRepository;
    private final JWTUtils jwtUtils;

    @Transactional(readOnly = true)
    public List<ClienteDTO> listar(String authorization) {
        Empresa empresa = empresaAutenticada(authorization);
        return clienteRepository.findByEmpresaNitAndActivoTrueOrderByNombreAsc(empresa.getNit())
                .stream().map(ClienteDTO::desde).toList();
    }

    public ClienteDTO crear(String authorization, ClienteCrearDTO dto) {
        Cliente cliente = new Cliente();
        cliente.setEmpresa(empresaAutenticada(authorization));
        cliente.setNombre(dto.nombre());
        cliente.setDocumento(dto.documento());
        cliente.setTelefono(dto.telefono());
        cliente.setCorreo(dto.correo());
        cliente.setDireccion(dto.direccion());
        return ClienteDTO.desde(clienteRepository.save(cliente));
    }

    public ClienteDTO actualizar(String authorization, Long id, ClienteActualizarDTO dto) {
        Empresa empresa = empresaAutenticada(authorization);
        Cliente cliente = clienteRepository.findByIdAndEmpresaNit(id, empresa.getNit())
                .filter(c -> Boolean.TRUE.equals(c.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado o inactivo en su empresa"));
        cliente.setNombre(dto.nombre());
        cliente.setDocumento(dto.documento());
        cliente.setTelefono(dto.telefono());
        cliente.setCorreo(dto.correo());
        cliente.setDireccion(dto.direccion());
        return ClienteDTO.desde(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public Empresa empresaAutenticada(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Debe proporcionar un token de acceso");
        }
        var claims = jwtUtils.parseJwt(authorization.substring(7)).getBody();
        String rol = claims.get("rol", String.class);
        Empresa empresa;
        if ("administrador".equals(rol)) {
            // El directorio es empresarial, incluso para administradores limitados.
            // No se admite una empresa solicitada ni una excepcion para superadmin.
            empresa = administradorRepository.findByCorreo(claims.getSubject())
                    .orElseThrow(() -> new IllegalArgumentException("Administrador no encontrado"))
                    .getEmpresa();
        } else if ("vendedor".equals(rol) || "produccion".equals(rol)) {
            Vendedor vendedor = vendedorRepository.findByCorreo(claims.getSubject())
                    .orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado"));
            boolean produccion = vendedor.getTipoPerfil() == TipoPerfilVendedor.PRODUCCION;
            if (produccion != "produccion".equals(rol)) {
                throw new IllegalArgumentException("El perfil de la cuenta no corresponde al token");
            }
            empresa = vendedor.getEmpresa() != null ? vendedor.getEmpresa()
                    : vendedor.getSede() != null ? vendedor.getSede().getEmpresa() : null;
        } else {
            throw new IllegalArgumentException("No tiene permisos para gestionar clientes");
        }
        if (empresa == null || empresa.getNit() == null) {
            throw new IllegalArgumentException("El usuario no tiene una empresa asociada");
        }
        return empresa;
    }
}
