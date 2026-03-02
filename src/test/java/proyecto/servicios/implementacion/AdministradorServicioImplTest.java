package proyecto.servicios.implementacion;

import com.cloudinary.Cloudinary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.UsuarioDTO;
import proyecto.entidades.Ciudad;
import proyecto.entidades.Empresa;
import proyecto.entidades.Sede;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
        UsuarioDTO dto = new UsuarioDTO(
                "123",
                "Vendedor",
                "Bogotá",
                "3000000000",
                "secret",
                "vendedor@correo.com",
                true,
                10L
        );

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
        UsuarioDTO dto = new UsuarioDTO(
                "123",
                "Vendedor",
                "Bogotá",
                "3000000000",
                "secret",
                "vendedor@correo.com",
                true,
                10L
        );

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
}
