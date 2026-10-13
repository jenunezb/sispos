package proyecto.controladores;

import org.junit.jupiter.api.Test;
import proyecto.dto.InventarioAjustableResponseDTO;
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.servicios.interfaces.InventarioServicio;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class InventarioControllerTest {

    private final InventarioServicio inventarios = mock(InventarioServicio.class);
    private final AdministradorAccesoService acceso = mock(AdministradorAccesoService.class);
    private final InventarioController controller = new InventarioController(inventarios, acceso);

    @Test
    void vendedorPuedeListarInventarioAjustableDeSuSede() {
        InventarioAjustableResponseDTO esperado = new InventarioAjustableResponseDTO(7L, List.of());
        when(inventarios.listarInventarioAjustablePorSede(7L)).thenReturn(esperado);

        var respuesta = controller.listarInventarioAjustablePorSede("Bearer token-vendedor", 7L);

        verify(acceso).validarAccesoAutenticadoASede("Bearer token-vendedor", 7L);
        assertSame(esperado, respuesta.getBody());
    }

    @Test
    void vendedorNoPuedeListarInventarioAjustableDeOtraSede() {
        doThrow(new RuntimeException("Sin acceso"))
                .when(acceso).validarAccesoAutenticadoASede("Bearer token-vendedor", 99L);

        assertThrows(RuntimeException.class,
                () -> controller.listarInventarioAjustablePorSede("Bearer token-vendedor", 99L));

        verifyNoInteractions(inventarios);
    }
}
