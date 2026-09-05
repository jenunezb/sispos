package proyecto.servicios.implementacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.InformeInventarioDiaDTO;
import proyecto.dto.InventarioDelDia;
import proyecto.dto.MateriaPrimaInventarioDTO;
import proyecto.entidades.InformeInventarioDia;
import proyecto.repositorios.InformeInventarioDiaRepository;
import proyecto.servicios.interfaces.InformeInventarioDiaService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class InformeInventarioDiaServiceImpl implements InformeInventarioDiaService {

    private final InformeInventarioDiaRepository repository;
    private final ObjectMapper objectMapper;
    private final InventarioServicioImpl inventarioServicio;

    @Override
    public InformeInventarioDia guardarInforme(InformeInventarioDiaDTO dto)
            throws JsonProcessingException {

        if (dto.totalVendido() == 0) {
            return null;
        }

        InformeInventarioDia informe = new InformeInventarioDia();
        informe.setSedeId(dto.sedeId());
        informe.setFecha(dto.fecha());
        informe.setTotalVendido(dto.totalVendido());

        InformeInventarioDia informeAnterior = repository.findBySedeIdAndFecha(dto.sedeId(), dto.fecha().minusDays(1))
                .stream()
                .findFirst()
                .orElse(null);

        List<InventarioDelDia> inventarioDia = normalizarInventarioDia(
                dto.inventarioDia(),
                obtenerCierreProductos(informeAnterior)
        );

        List<MateriaPrimaInventarioDTO> materiaPrimaDia = normalizarMateriaPrimaDia(
                resolverMateriaPrimaDia(dto),
                obtenerCierreMateriaPrima(informeAnterior)
        );

        Map<String, Object> datosInforme = new HashMap<>();
        datosInforme.put("productos", inventarioDia);
        datosInforme.put("materiaPrima", materiaPrimaDia);

        informe.setDatosJson(objectMapper.writeValueAsString(datosInforme));
        return repository.save(informe);
    }

    @Override
    public List<InformeInventarioDia> obtenerInformes(Long sedeId, LocalDate fecha) {
        return repository.findBySedeIdAndFecha(sedeId, fecha);
    }

    private List<MateriaPrimaInventarioDTO> resolverMateriaPrimaDia(InformeInventarioDiaDTO dto) {
        if (dto.materiaPrimaDia() != null && !dto.materiaPrimaDia().isEmpty()) {
            return dto.materiaPrimaDia();
        }

        return inventarioServicio.obtenerInventarioMateriaPrimaDia(
                dto.sedeId(),
                dto.fecha().atStartOfDay(),
                dto.fecha().atTime(23, 59, 59)
        );
    }

    private List<InventarioDelDia> normalizarInventarioDia(
            List<InventarioDelDia> inventarioDia,
            Map<Long, Integer> cierreProductosAnterior
    ) {
        if (inventarioDia == null || inventarioDia.isEmpty() || cierreProductosAnterior.isEmpty()) {
            return inventarioDia;
        }

        return inventarioDia.stream()
                .map(item -> {
                    Integer cierreAnterior = cierreProductosAnterior.get(item.productoId());
                    if (cierreAnterior == null) {
                        return item;
                    }

                    return new InventarioDelDia(
                            item.productoId(),
                            item.productoNombre(),
                            cierreAnterior,
                            item.entradas(),
                            item.salidas(),
                            item.perdidas(),
                            item.ventasDelDia(),
                            item.stockActual(),
                            item.precio(),
                            item.totalVendido()
                    );
                })
                .toList();
    }

    private List<MateriaPrimaInventarioDTO> normalizarMateriaPrimaDia(
            List<MateriaPrimaInventarioDTO> materiaPrimaDia,
            Map<Long, Double> cierreMateriaPrimaAnterior
    ) {
        if (materiaPrimaDia == null || materiaPrimaDia.isEmpty() || cierreMateriaPrimaAnterior.isEmpty()) {
            return materiaPrimaDia;
        }

        return materiaPrimaDia.stream()
                .map(item -> {
                    Double cierreAnterior = cierreMateriaPrimaAnterior.get(item.codigo());
                    if (cierreAnterior == null) {
                        return item;
                    }

                    return new MateriaPrimaInventarioDTO(
                            item.codigo(),
                            item.nombre(),
                            cierreAnterior,
                            item.entradas(),
                            item.salidas(),
                            item.perdidas(),
                            item.vendidas(),
                            item.stockActual()
                    );
                })
                .toList();
    }

    private Map<Long, Integer> obtenerCierreProductos(InformeInventarioDia informeAnterior) {
        Map<Long, Integer> cierre = new LinkedHashMap<>();
        JsonNode productos = obtenerArrayDesdeInforme(informeAnterior, "productos");
        if (productos == null) {
            return cierre;
        }

        for (JsonNode producto : productos) {
            JsonNode codigo = producto.get("productoId");
            JsonNode stockActual = producto.get("stockActual");
            if (codigo != null && stockActual != null && codigo.canConvertToLong() && stockActual.canConvertToInt()) {
                cierre.put(codigo.longValue(), stockActual.intValue());
            }
        }

        return cierre;
    }

    private Map<Long, Double> obtenerCierreMateriaPrima(InformeInventarioDia informeAnterior) {
        Map<Long, Double> cierre = new LinkedHashMap<>();
        JsonNode materiasPrimas = obtenerArrayDesdeInforme(informeAnterior, "materiaPrima");
        if (materiasPrimas == null) {
            return cierre;
        }

        for (JsonNode materiaPrima : materiasPrimas) {
            JsonNode codigo = materiaPrima.get("codigo");
            JsonNode stockActual = materiaPrima.get("stockActual");
            if (codigo != null && stockActual != null && codigo.canConvertToLong() && stockActual.isNumber()) {
                cierre.put(codigo.longValue(), stockActual.doubleValue());
            }
        }

        return cierre;
    }

    private JsonNode obtenerArrayDesdeInforme(InformeInventarioDia informeAnterior, String campo) {
        if (informeAnterior == null || informeAnterior.getDatosJson() == null || informeAnterior.getDatosJson().isBlank()) {
            return null;
        }

        try {
            JsonNode raiz = objectMapper.readTree(informeAnterior.getDatosJson());
            if (raiz.isArray()) {
                return "productos".equals(campo) ? raiz : null;
            }

            JsonNode nodo = raiz.path(campo);
            return nodo.isArray() ? nodo : null;
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }
}
