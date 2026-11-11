package proyecto.servicios.implementacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import proyecto.dto.InformeInventarioDiaDTO;
import proyecto.dto.InventarioDelDia;
import proyecto.dto.MateriaPrimaInventarioDTO;
import proyecto.entidades.InformeInventarioDia;
import proyecto.repositorios.InformeInventarioDiaRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InformeInventarioDiaServiceImplTest {

    @Mock
    private InformeInventarioDiaRepository repository;

    @Mock
    private InventarioServicioImpl inventarioServicio;

    private InformeInventarioDiaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InformeInventarioDiaServiceImpl(repository, new ObjectMapper(), inventarioServicio);
    }

    @Test
    void guardarInformeDebeArrastrarElCierreAnteriorEnProductosYMateriaPrima() throws Exception {
        InformeInventarioDia anterior = new InformeInventarioDia();
        anterior.setDatosJson("""
                {
                  "productos": [
                    {
                      "productoId": 77,
                      "stockActual": 324
                    }
                  ],
                  "materiaPrima": [
                    {
                      "codigo": 88,
                      "stockActual": 324
                    }
                  ]
                }
                """);

        when(repository.findBySedeIdAndFecha(5L, LocalDate.of(2026, 7, 7)))
                .thenReturn(List.of(anterior));
        when(repository.save(any(InformeInventarioDia.class))).thenAnswer(inv -> inv.getArgument(0));

        InformeInventarioDiaDTO dto = new InformeInventarioDiaDTO(
                5L,
                LocalDate.of(2026, 7, 8),
                List.of(new InventarioDelDia(77L, "Pasta milhojas", 0, 432, 0, 0, 108, 324, 1000D, 108000D)),
                List.of(new MateriaPrimaInventarioDTO(88L, "Pasta milhojas", 0, 432, 0, 0, 108, 324)),
                108000D
        );

        service.guardarInforme(dto);

        ArgumentCaptor<InformeInventarioDia> captor = ArgumentCaptor.forClass(InformeInventarioDia.class);
        verify(repository).save(captor.capture());
        verify(inventarioServicio, never()).obtenerInventarioMateriaPrimaDia(any(), any(), any());

        JsonNode json = new ObjectMapper().readTree(captor.getValue().getDatosJson());
        assertEquals(324, json.path("productos").get(0).path("stockInicial").asInt());
        assertEquals(324D, json.path("materiaPrima").get(0).path("stockInicial").asDouble());
        assertEquals(432D, json.path("materiaPrima").get(0).path("entradas").asDouble());
        assertEquals(324D, json.path("materiaPrima").get(0).path("stockActual").asDouble());
    }
}
