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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
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
        vendedor.setTipoPerfil(parsePerfil(usuarioDTO.perfil()));
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

    @Override
    @Transactional
    public int crearAdministradorDelegado(AdministradorEmpresaCrearDTO dto, Integer administradorDeleganteCodigo) throws Exception {
        if (estaRepetidoCorreo(dto.correo())) {
            throw new Exception("El correo ya se encuentra registrado");
        }

        Administrador administradorDelegante = administradorRepository.findDetalleByCodigo(administradorDeleganteCodigo)
                .orElseThrow(() -> new RuntimeException("Administrador delegante no encontrado"));

        if (administradorDelegante.getEmpresa() == null) {
            throw new RuntimeException("El administrador delegante no tiene una empresa asociada");
        }

        Empresa empresa = empresaRepository.findById(administradorDelegante.getEmpresa().getNit())
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        List<Sede> sedesAsignadas = obtenerSedesValidasDeEmpresa(dto.sedeIds(), empresa.getNit());

        Administrador admin = new Administrador();
        admin.setCorreo(dto.correo());
        admin.setPassword(passwordEncoder.encode(dto.password()));
        admin.setNombre(dto.nombre());
        admin.setApellido(dto.apellido());
        admin.setCelular(dto.celular());
        admin.setEmpresa(empresa);
        admin.setEsSuperAdmin(false);
        admin.setEsAdministradorEmpresa(false);
        admin.setSedesAsignadas(new ArrayList<>(sedesAsignadas));

        administradorRepository.save(admin);
        return admin.getCodigo();
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


    private TipoPerfilVendedor parsePerfil(String perfil) {
        if (perfil == null || perfil.isBlank()) {
            return TipoPerfilVendedor.VENDEDOR;
        }

        if ("PRODUCCION".equalsIgnoreCase(perfil.trim())) {
            return TipoPerfilVendedor.PRODUCCION;
        }

        return TipoPerfilVendedor.VENDEDOR;
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
    public List<AdministradorEmpresaDTO> listarAdministradoresEmpresa(Long empresaNit) {
        return administradorRepository.findDetalleByEmpresaNit(empresaNit)
                .stream()
                .map(admin -> new AdministradorEmpresaDTO(
                        admin.getCodigo(),
                        admin.getNombre(),
                        admin.getApellido(),
                        admin.getCorreo(),
                        admin.getCelular(),
                        admin.isEsAdministradorEmpresa(),
                        admin.getSedesAsignadas().stream()
                                .map(Sede::getId)
                                .sorted()
                                .toList()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void actualizarSedesAdministrador(Integer administradorCodigo, AdministradorSedesDTO dto, Long empresaNit) {
        Administrador admin = administradorRepository.findDetalleByCodigo(administradorCodigo)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));

        if (admin.isEsSuperAdmin()) {
            throw new RuntimeException("No se pueden modificar las sedes del administrador del sistema");
        }

        if (admin.getEmpresa() == null || !empresaNit.equals(admin.getEmpresa().getNit())) {
            throw new RuntimeException("El administrador no pertenece a la empresa indicada");
        }

        if (admin.isEsAdministradorEmpresa()) {
            throw new RuntimeException("Las sedes del administrador principal no se gestionan manualmente");
        }

        admin.setSedesAsignadas(new ArrayList<>(obtenerSedesValidasDeEmpresa(dto.sedeIds(), empresaNit)));
        administradorRepository.save(admin);
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

    @Override
    @Transactional
    public String actualizarLogoEmpresa(String correo, MultipartFile logo) throws Exception {
        if (logo == null || logo.isEmpty()) {
            throw new RuntimeException("Debe seleccionar una imagen");
        }

        Administrador admin = administradorRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));

        if (admin.getEmpresa() == null) {
            throw new RuntimeException("El administrador no tiene empresa asociada");
        }

        Empresa empresa = empresaRepository.findById(admin.getEmpresa().getNit())
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Imagen anterior = empresa.getLogo();

        Map<String, Object> opciones = new HashMap<>();
        opciones.put("folder", "logos_empresas");

        Map<?, ?> resultado = cloudinary.uploader().upload(logo.getBytes(), opciones);

        Imagen nuevaImagen = new Imagen();
        nuevaImagen.setUrl(resultado.get("secure_url").toString());
        nuevaImagen.setPublicId(resultado.get("public_id").toString());
        nuevaImagen.setTipo(TipoImagen.LOGO);
        imagenRepository.save(nuevaImagen);

        empresa.setLogo(nuevaImagen);
        empresaRepository.save(empresa);

        if (anterior != null) {
            try {
                cloudinary.uploader().destroy(anterior.getPublicId(), Map.of());
            } catch (Exception ignored) {
                // Si falla Cloudinary, al menos dejamos la referencia nueva guardada.
            }
            imagenRepository.delete(anterior);
        }

        return "Logo actualizado correctamente";
    }

    @Override
    public String obtenerLogoEmpresa(String correo) {
        Empresa empresa = obtenerEmpresaPorCorreo(correo);
        return empresa.getLogo() != null ? empresa.getLogo().getUrl() : null;
    }

    @Override
    @Transactional
    public String actualizarImpresionCocinaHabilitada(String correo, Boolean habilitada) {
        if (habilitada == null) {
            throw new RuntimeException("El estado de impresion de cocina es obligatorio");
        }

        Empresa empresa = obtenerEmpresaPorCorreo(correo);
        empresa.setImpresionCocinaHabilitada(habilitada);
        empresaRepository.save(empresa);

        return habilitada
                ? "Impresion de cocina habilitada correctamente"
                : "Impresion de cocina deshabilitada correctamente";
    }

    @Override
    public Boolean obtenerImpresionCocinaHabilitada(String correo) {
        Empresa empresa = obtenerEmpresaPorCorreo(correo);
        return !Boolean.FALSE.equals(empresa.getImpresionCocinaHabilitada());
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
        empresa.setDv(normalizarDv(dto.dv()));
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
        admin.setEsAdministradorEmpresa(true);

        administradorRepository.save(admin);

        // 4️⃣ Crear sede principal
        Sede sede = new Sede();
        sede.setUbicacion(dto.ubicacionSede());
        sede.setEmpresa(empresa);
        sede.setAdministrador(admin);

        sedeRepository.save(sede);
        admin.setSedesAsignadas(new ArrayList<>(List.of(sede)));
        administradorRepository.save(admin);

        return admin.getCodigo();
    }

    @Override
    @Transactional
    public int registrarAdministradorSistema(RegistroAdministradorSistemaDTO dto) throws Exception {

        if (administradorRepository.countByEsSuperAdminTrue() > 0) {
            throw new RuntimeException("El administrador del sistema ya fue configurado");
        }

        if (estaRepetidoCorreo(dto.correo())) {
            throw new Exception("El correo ya estÃ¡ registrado");
        }

        Administrador admin = new Administrador();
        admin.setCorreo(dto.correo());
        admin.setPassword(passwordEncoder.encode(dto.password()));
        admin.setNombre(dto.nombre());
        admin.setApellido(dto.apellido());
        admin.setCelular(dto.celular());
        admin.setEsSuperAdmin(true);
        admin.setEsAdministradorEmpresa(false);
        admin.setEmpresa(null);

        administradorRepository.save(admin);
        return admin.getCodigo();
    }

    private List<Sede> obtenerSedesValidasDeEmpresa(List<Long> sedeIds, Long empresaNit) {
        List<Sede> sedes = sedeRepository.findByEmpresaNitAndIdIn(empresaNit, sedeIds);

        if (sedes.size() != sedeIds.size()) {
            throw new RuntimeException("Una o mas sedes no pertenecen a la empresa");
        }

        return sedes;
    }

    private Empresa obtenerEmpresaPorCorreo(String correo) {
        Optional<Administrador> adminOpt = administradorRepository.findByCorreoIgnoreCase(correo);
        if (adminOpt.isPresent()) {
            Administrador admin = adminOpt.get();
            if (admin.getEmpresa() == null) {
                throw new RuntimeException("La cuenta no tiene empresa asociada");
            }
            return empresaRepository.findById(admin.getEmpresa().getNit())
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        }

        Optional<Vendedor> vendedorOpt = vendedorRepository.findByCorreoIgnoreCase(correo);
        if (vendedorOpt.isPresent()) {
            Vendedor vendedor = vendedorOpt.get();
            Empresa empresa = vendedor.getEmpresa();

            if (empresa == null && vendedor.getSede() != null) {
                empresa = vendedor.getSede().getEmpresa();
            }

            if (empresa == null) {
                throw new RuntimeException("La cuenta no tiene empresa asociada");
            }

            Long nit = empresa.getNit();
            return empresaRepository.findById(nit)
                    .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        }

        throw new RuntimeException("Cuenta no encontrada");
    }

    private String normalizarDv(String dv) {
        String dvNormalizado = dv == null ? "" : dv.trim();
        return dvNormalizado.isEmpty() ? null : dvNormalizado;
    }

}
