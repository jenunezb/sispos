package proyecto.servicios.implementacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.VendedorDTO;
import proyecto.entidades.Vendedor;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VendedorRepository;
import proyecto.repositorios.VentaRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendedorServicioImplTest {

    @Mock
    private VendedorRepository vendedorRepository;

    @Mock
    private SedeRepository sedeRepository;

    @Mock
    private VentaRepository ventaRepository;

    @InjectMocks
    private VendedorServicioImpl vendedorServicio;

    @Test
    void listarVendedoresDebeSoportarCiudadNula() {
        Vendedor vendedor = new Vendedor();
        vendedor.setCodigo(1);
        vendedor.setNombre("Juan");
        vendedor.setCedula("123");
        vendedor.setCorreo("juan@correo.com");
        vendedor.setTelefono("3001234567");
        vendedor.setEstado(true);
        vendedor.setCiudad(null);

        when(vendedorRepository.findVisiblesByEmpresaNit(900123456L)).thenReturn(List.of(vendedor));

        List<VendedorDTO> respuesta = vendedorServicio.listarVendedores(900123456L);

        assertEquals(1, respuesta.size());
        assertEquals("SIN CIUDAD", respuesta.get(0).ciudad());
    }
}
