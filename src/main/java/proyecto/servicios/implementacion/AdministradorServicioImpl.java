package proyecto.servicios.implementacion;

import com.cloudinary.Cloudinary;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import proyecto.dto.*;
import proyecto.entidades.*;
import proyecto.repositorios.*;
import proyecto.servicios.interfaces.AdministradorServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdministradorServicioImpl implements AdministradorServicio {

    private final AdministradorRepository administradorRepository;
    private final VendedorRepository vendedorRepository;
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final CiudadRepo ciudadRepo;
    private final CuentaRepo cuentaRepo;
    private final ImagenRepository imagenRepository;
    private final EmpresaRepository empresaRepository;
    private final Cloudinary cloudinary;
    private final SedeRepository sedeRepository;


    @Override
    public int crearVendedor(UsuarioDTO usuarioDTO, Long empresaNit) throws Exception {
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
        vendedor.setCiudad(obtenerCiudadParaVendedor(usuarioDTO.ciudad()));
        if (vendedor.getCiudad() == null) {
            vendedor.setCiudad(obtenerCiudadParaVendedor(null));
        }
        vendedor.setCorreo(usuarioDTO.correo());
        vendedor.setEstado(true);
        String passwordEncriptada = passwordEncoder.encode(usuarioDTO.password());
        vendedor.setPassword(passwordEncriptada);

        Empresa empresa = empresaRepository.findById(empresaNit)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        if (usuarioDTO.sedeId() == null) {
            throw new RuntimeException("Debe seleccionar una sede para el vendedor");
        }

        Sede sede = sedeRepository.findById(usuarioDTO.sedeId())
                .orElseThrow(() -> new RuntimeException("Sede no encontrada"));

        if (sede.getEmpresa() == null || !empresaNit.equals(sede.getEmpresa().getNit())) {
            throw new RuntimeException("La sede no pertenece a la empresa del administrador");
        }

        vendedor.setEmpresa(empresa);
        vendedor.setSede(sede);

        Vendedor vendedorNuevo = vendedorRepository.save(vendedor);

        return vendedorNuevo.getCodigo();
    }


    private Ciudad obtenerCiudadParaVendedor(String nombreCiudad) {
        if (nombreCiudad != null && !nombreCiudad.isBlank()) {
            Ciudad ciudad = ciudadRepo.findByNombre(nombreCiudad.trim());
            if (ciudad != null) {
                return ciudad;
            }

            Ciudad nuevaCiudad = new Ciudad();
            nuevaCiudad.setNombre(nombreCiudad.trim());
            return ciudadRepo.save(nuevaCiudad);
        }

        String nombreDefault = "SIN CIUDAD";
        Ciudad ciudadDefault = ciudadRepo.findByNombre(nombreDefault);
        if (ciudadDefault != null) {
            return ciudadDefault;
        }

        Ciudad nuevaCiudadDefault = new Ciudad();
        nuevaCiudadDefault.setNombre(nombreDefault);
        return ciudadRepo.save(nuevaCiudadDefault);
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

    @Override
    public void editarVendedor(UsuarioDTO dto) {

        Vendedor vendedor = vendedorRepository.findByCedula(dto.cedula())
                .orElseThrow(() ->
                        new EntityNotFoundException("Vendedor no encontrado")
                );

        vendedor.setNombre(dto.nombre());
        vendedor.setCorreo(dto.correo());
        vendedor.setTelefono(dto.telefono());
        vendedor.setCiudad(obtenerCiudadParaVendedor(dto.ciudad()));
        if (vendedor.getCiudad() == null) {
            vendedor.setCiudad(obtenerCiudadParaVendedor(null));
        }
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

    @Transactional
    public int registrarEmpresa(RegistroEmpresaDTO dto, MultipartFile archivo) throws Exception {

        if (empresaRepository.existsById(dto.nit())) {
            throw new Exception("Ya existe una empresa con ese NIT");
        }

        if (estaRepetidoCorreo(dto.correo())) {
            throw new Exception("El correo ya está registrado");
        }

        Imagen imagen = null;

        if (archivo != null && !archivo.isEmpty()) {
            Map<?, ?> resultado = cloudinary.uploader().upload(
                    archivo.getBytes(),
                    Map.of("folder", "logos_empresas")
            );

            imagen = new Imagen();
            imagen.setUrl(resultado.get("secure_url").toString());
            imagen.setPublicId(resultado.get("public_id").toString());
            imagen.setTipo(TipoImagen.LOGO);

            imagenRepository.save(imagen);
        }

        // 2️⃣ Crear empresa
        Empresa empresa = new Empresa();
        empresa.setNit(dto.nit());
        empresa.setNombre(dto.nombreEmpresa());
        empresa.setLogo(imagen);

        empresaRepository.save(empresa);

        // 3️⃣ Crear administrador
        Administrador admin = new Administrador();
        admin.setCorreo(dto.correo());
        admin.setPassword(passwordEncoder.encode(dto.password()));
        admin.setNombre(dto.nombre());
        admin.setApellido(dto.apellido());
        admin.setCelular(dto.celular());
        admin.setEmpresa(empresa);

        administradorRepository.save(admin);

        // 4️⃣ Crear sede principal
        Sede sede = new Sede();
        sede.setUbicacion(dto.ubicacionSede());
        sede.setEmpresa(empresa);
        sede.setAdministrador(admin);

        sedeRepository.save(sede);

        return admin.getCodigo();
    }

}