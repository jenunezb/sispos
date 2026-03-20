package proyecto.servicios.implementacion;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.AdministradorEmpresaCrearDTO;
import proyecto.dto.AdministradorSedesDTO;
import proyecto.dto.RegistroEmpresaDTO;
import proyecto.dto.UsuarioDTO;
import proyecto.entidades.Administrador;
import proyecto.entidades.Ciudad;
import proyecto.entidades.Empresa;
import proyecto.entidades.Sede;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.AdministradorRepository;
import proyecto.repositorios.CiudadRepo;
import proyecto.repositorios.CuentaRepo;
import proyecto.repositorios.EmpresaRepository;
import proyecto.repositorios.ImagenRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministradorServicioImplTest {

    @Mock
    private AdministradorRepository administradorRepository;
    @Mock
    private VendedorRepository vendedorRepository;
    @Mock
    private CiudadRepo ciudadRepo;
    @Mock
    private CuentaRepo cuentaRepo;
    @Mock
    private ImagenRepository imagenRepository;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private Cloudinary cloudinary;
    @Mock
    private SedeRepository sedeRepository;

    @InjectMocks
    private AdministradorServicioImpl administradorServicio;

    @Test
    void crearVendedorDebeAsignarSedeDeLaEmpresa() throws Exception {
        UsuarioDTO dto = new UsuarioDTO("123", "Vendedor", "Bogotá", "3000000000", "secret", "vendedor@correo.com", true, 10L, null);

        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Sede sede = new Sede();
        sede.setId(10L);
        sede.setEmpresa(empresa);

        Ciudad ciudad = new Ciudad();
        ciudad.setNombre("Bogotá");

        when(vendedorRepository.existsByCedula("123")).thenReturn(false);
        when(cuentaRepo.findByCorreo("vendedor@correo.com")).thenReturn(Optional.empty());
        when(ciudadRepo.findByNombre("Bogotá")).thenReturn(ciudad);
        when(empresaRepository.findById(900123456L)).thenReturn(Optional.of(empresa));
        when(sedeRepository.findById(10L)).thenReturn(Optional.of(sede));
        when(vendedorRepository.save(any(Vendedor.class))).thenAnswer(inv -> {
            Vendedor v = inv.getArgument(0);
            v.setCodigo(1);
            return v;
        });

        administradorServicio.crearVendedor(dto, 900123456L);

        ArgumentCaptor<Vendedor> captor = ArgumentCaptor.forClass(Vendedor.class);
        verify(vendedorRepository).save(captor.capture());
        assertEquals(empresa, captor.getValue().getEmpresa());
        assertEquals(sede, captor.getValue().getSede());
    }

    @Test
    void crearVendedorDebeFallarSiSedeNoPerteneceAEmpresa() {
        UsuarioDTO dto = new UsuarioDTO("123", "Vendedor", "Bogotá", "3000000000", "secret", "vendedor@correo.com", true, 10L, null);

        Empresa empresaAdmin = new Empresa();
        empresaAdmin.setNit(900123456L);

        Empresa otraEmpresa = new Empresa();
        otraEmpresa.setNit(800111222L);

        Sede sede = new Sede();
        sede.setId(10L);
        sede.setEmpresa(otraEmpresa);

        Ciudad ciudad = new Ciudad();
        ciudad.setNombre("Bogotá");

        when(vendedorRepository.existsByCedula("123")).thenReturn(false);
        when(cuentaRepo.findByCorreo("vendedor@correo.com")).thenReturn(Optional.empty());
        when(ciudadRepo.findByNombre("Bogotá")).thenReturn(ciudad);
        when(empresaRepository.findById(900123456L)).thenReturn(Optional.of(empresaAdmin));
        when(sedeRepository.findById(10L)).thenReturn(Optional.of(sede));

        assertThrows(RuntimeException.class, () -> administradorServicio.crearVendedor(dto, 900123456L));
    }

    @Test
    void registrarEmpresaDebePermitirLogoOpcional() throws Exception {
        RegistroEmpresaDTO dto = new RegistroEmpresaDTO(
                "admin@correo.com",
                "Password#123",
                "Admin",
                "Principal",
                3001234567L,
                900123456L,
                "Empresa Demo",
                "Sede Principal",
                "Centro"
        );

        when(empresaRepository.existsById(900123456L)).thenReturn(false);
        when(cuentaRepo.findByCorreo("admin@correo.com")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(administradorRepository.save(any(Administrador.class))).thenAnswer(inv -> {
            Administrador admin = inv.getArgument(0);
            admin.setCodigo(1);
            return admin;
        });
        when(sedeRepository.save(any(Sede.class))).thenAnswer(inv -> inv.getArgument(0));

        int codigo = administradorServicio.registrarEmpresa(dto, null);

        assertEquals(1, codigo);

        ArgumentCaptor<Empresa> empresaCaptor = ArgumentCaptor.forClass(Empresa.class);
        verify(empresaRepository).save(empresaCaptor.capture());
        assertEquals(null, empresaCaptor.getValue().getLogo());
        verify(imagenRepository, never()).save(any());
    }

    @Test
    void registrarEmpresaDebeMarcarAdministradorPrincipal() throws Exception {
        RegistroEmpresaDTO dto = new RegistroEmpresaDTO(
                "admin@correo.com",
                "Password#123",
                "Admin",
                "Principal",
                3001234567L,
                900123456L,
                "Empresa Demo",
                "Sede Principal",
                "Centro"
        );

        when(empresaRepository.existsById(900123456L)).thenReturn(false);
        when(cuentaRepo.findByCorreo("admin@correo.com")).thenReturn(Optional.empty());
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(administradorRepository.save(any(Administrador.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sedeRepository.save(any(Sede.class))).thenAnswer(inv -> {
            Sede sede = inv.getArgument(0);
            sede.setId(1L);
            return sede;
        });

        administradorServicio.registrarEmpresa(dto, null);

        ArgumentCaptor<Administrador> captor = ArgumentCaptor.forClass(Administrador.class);
        verify(administradorRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());

        Administrador ultimoGuardado = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertTrue(ultimoGuardado.isEsAdministradorEmpresa());
        assertEquals(1, ultimoGuardado.getSedesAsignadas().size());
        assertEquals(1L, ultimoGuardado.getSedesAsignadas().get(0).getId());
    }

    @Test
    void crearAdministradorDelegadoDebeAsignarSedes() throws Exception {
        AdministradorEmpresaCrearDTO dto = new AdministradorEmpresaCrearDTO(
                "delegado@correo.com",
                "Password#123",
                "Laura",
                "Lopez",
                3001234567L,
                List.of(10L, 11L)
        );

        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Administrador principal = new Administrador();
        principal.setCodigo(99);
        principal.setEmpresa(empresa);
        principal.setEsAdministradorEmpresa(true);

        Sede sede1 = new Sede();
        sede1.setId(10L);
        sede1.setEmpresa(empresa);

        Sede sede2 = new Sede();
        sede2.setId(11L);
        sede2.setEmpresa(empresa);

        when(cuentaRepo.findByCorreo("delegado@correo.com")).thenReturn(Optional.empty());
        when(administradorRepository.findDetalleByCodigo(99)).thenReturn(Optional.of(principal));
        when(empresaRepository.findById(900123456L)).thenReturn(Optional.of(empresa));
        when(sedeRepository.findByEmpresaNitAndIdIn(900123456L, List.of(10L, 11L))).thenReturn(List.of(sede1, sede2));
        when(administradorRepository.save(any(Administrador.class))).thenAnswer(inv -> {
            Administrador admin = inv.getArgument(0);
            admin.setCodigo(5);
            return admin;
        });

        int codigo = administradorServicio.crearAdministradorDelegado(dto, 99);

        assertEquals(5, codigo);

        ArgumentCaptor<Administrador> captor = ArgumentCaptor.forClass(Administrador.class);
        verify(administradorRepository).save(captor.capture());
        assertFalse(captor.getValue().isEsAdministradorEmpresa());
        assertEquals(2, captor.getValue().getSedesAsignadas().size());
    }

    @Test
    void actualizarSedesAdministradorDebeReemplazarAsignaciones() {
        Empresa empresa = new Empresa();
        empresa.setNit(900123456L);

        Administrador delegado = new Administrador();
        delegado.setCodigo(7);
        delegado.setEmpresa(empresa);
        delegado.setEsAdministradorEmpresa(false);

        Sede sede = new Sede();
        sede.setId(20L);
        sede.setEmpresa(empresa);

        when(administradorRepository.findDetalleByCodigo(7)).thenReturn(Optional.of(delegado));
        when(sedeRepository.findByEmpresaNitAndIdIn(900123456L, List.of(20L))).thenReturn(List.of(sede));
        when(administradorRepository.save(any(Administrador.class))).thenAnswer(inv -> inv.getArgument(0));

        administradorServicio.actualizarSedesAdministrador(7, new AdministradorSedesDTO(List.of(20L)), 900123456L);

        assertEquals(1, delegado.getSedesAsignadas().size());
        assertEquals(20L, delegado.getSedesAsignadas().get(0).getId());
    }
}
