package proyecto.servicios.implementacion;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import proyecto.dto.AdministradorDTO;
import proyecto.dto.InventarioFinalDTO;
import proyecto.dto.InventarioFinalProjection;
import proyecto.dto.UsuarioDTO;
import proyecto.entidades.Administrador;
import proyecto.entidades.Cuenta;
import proyecto.entidades.TokenValidacion;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.*;
import proyecto.servicios.interfaces.AdministradorServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdministradorServicioImpl implements AdministradorServicio {

    private final AdministradorRepository administradorRepository;
    private final VendedorRepository vendedorRepository;
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final CiudadRepo ciudadRepo;
    private final CuentaRepo cuentaRepo;
    private final EmailService emailService;
    private final TokenValidacionRepository tokenValidacionRepository;

    @Override
    public int crearVendedor(UsuarioDTO usuarioDTO) throws Exception {
        if (vendedorRepository.existsByCedula(usuarioDTO.cedula())) {
            throw new RuntimeException("La cédula ya se encuentra registrada");
        }

        if (estaRepetidoCorreo(usuarioDTO.correo())) {
            throw new Exception("El correo ya se encuentra registrado");
        }

        Vendedor vendedor = new Vendedor();
        vendedor.setCedula(usuarioDTO.cedula());
        vendedor.setNombre(usuarioDTO.nombre());
        vendedor.setTelefono(usuarioDTO.telefono());
        vendedor.setCiudad(ciudadRepo.findByNombre(usuarioDTO.ciudad()));
        vendedor.setCorreo(usuarioDTO.correo());
        vendedor.setEstado(true);
        String passwordEncriptada = passwordEncoder.encode(usuarioDTO.password());
        vendedor.setPassword(passwordEncriptada);

        Vendedor vendedorNuevo = vendedorRepository.save(vendedor);

        return vendedorNuevo.getCodigo();
    }

    public boolean estaRepetidaCedula(String cedula) {
        Optional<Vendedor> digitadorBuscado = vendedorRepository.findByCedula(cedula);
        if (!digitadorBuscado.isEmpty()) {
            if (!digitadorBuscado.get().isEstado()) {
                return false;
            }
            return true;
        }
        return vendedorRepository.existsByCedula(cedula);
    }

    public boolean estaRepetidoCorreo(String correo) {
        Optional<Cuenta> cuenta = cuentaRepo.findByCorreo(correo);
        return cuenta.isPresent(); // Devuelve true si la cuenta está presente (correo repetido), false si no está presente
    }

    @Transactional
    @Override
    public int crearAdministrador(AdministradorDTO administradorDTO) {

        try {
            // 1️⃣ Crear administrador (INACTIVO)
            Administrador admin = new Administrador();
            admin.setCorreo(administradorDTO.correo().toLowerCase().trim());
            admin.setPassword(passwordEncoder.encode(administradorDTO.password()));
            admin.setActivo(false);

            Administrador adminGuardado = administradorRepository.save(admin);

            // 2️⃣ Generar token
            String token = UUID.randomUUID().toString();

            TokenValidacion tokenValidacion = new TokenValidacion();
            tokenValidacion.setToken(token);
            tokenValidacion.setAdministrador(adminGuardado);
            tokenValidacion.setFechaExpiracion(LocalDateTime.now().plusHours(24));

            tokenValidacionRepository.save(tokenValidacion);

            //3️⃣ Enviar correo
            String link = "http://localhost:8080/api/auth/confirmar?token=" + token;

            emailService.enviarCorreo(
                    adminGuardado.getCorreo(),
                    "Confirmación de correo",
                    "Haz clic para activar tu cuenta:\n\n" + link
            );

            return adminGuardado.getCodigo();

        } catch (Exception e) {
            // 🔥 Rollback automático
            throw new RuntimeException(
                    "Error creando el administrador o enviando el correo de confirmación"
            );
        }
    }



    @Override
    public void editarVendedor(UsuarioDTO dto) {

        Vendedor vendedor = vendedorRepository.findByCedula(dto.cedula())
                .orElseThrow(() ->
                        new EntityNotFoundException("Vendedor no encontrado")
                );

        vendedor.setNombre(dto.nombre());
        vendedor.setCorreo(dto.correo());
        vendedor.setTelefono(dto.telefono());
        vendedor.setEstado(dto.estado());

        if (dto.password() != null && !dto.password().isBlank()) {
            vendedor.setPassword(passwordEncoder.encode(dto.password()));
        }

        vendedorRepository.save(vendedor);
    }

    @Override
    public List<InventarioFinalDTO> obtenerInventarioFinal(
            Long sedeId,
            LocalDate fechaInicio,
            LocalDate fechaFin
    ) {

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(23, 59, 59);

        return administradorRepository
                .obtenerInventarioFinal(sedeId, inicio, fin)
                .stream()
                .map(p -> new InventarioFinalDTO(
                        p.getSedeId(),
                        p.getProductoNombre(),
                        p.getInventarioInicial(),
                        p.getEntradas(),
                        p.getTotal(),
                        p.getInventarioFinal(),
                        p.getCantVendida(),
                        p.getPrecio(),
                        p.getTotalVendido()
                ))
                .toList();
    }

    @Override
    public void cambiarPassword(String correo, String passwordActual, String passwordNueva) throws Exception {

        Cuenta cuenta = cuentaRepo.findByCorreo(correo)
                .orElseThrow(() -> new Exception("La cuenta no existe"));

        // 🔐 Verificar contraseña actual
        if (!passwordEncoder.matches(passwordActual, cuenta.getPassword())) {
            throw new Exception("La contraseña actual es incorrecta");
        }

        // 🔎 Validar nueva contraseña
        String regexPassword = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._#\\-]).{8,}$";

        if (!passwordNueva.matches(regexPassword)) {
            throw new Exception(
                    "La nueva contraseña debe tener mínimo 8 caracteres, una mayúscula, un número y un carácter especial"
            );
        }

        // 🚫 Evitar que sea la misma contraseña
        if (passwordEncoder.matches(passwordNueva, cuenta.getPassword())) {
            throw new Exception("La nueva contraseña no puede ser igual a la actual");
        }

        // 🔐 Encriptar y guardar
        cuenta.setPassword(passwordEncoder.encode(passwordNueva));
        cuentaRepo.save(cuenta);
    }

}


