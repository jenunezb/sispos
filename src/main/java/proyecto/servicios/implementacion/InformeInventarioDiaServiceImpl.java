package proyecto.servicios.implementacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.InformeInventarioDiaDTO;
import proyecto.dto.MateriaPrimaInventarioDTO;
import proyecto.entidades.InformeInventarioDia;
import proyecto.repositorios.InformeInventarioDiaRepository;
import proyecto.servicios.interfaces.InformeInventarioDiaService;
import java.util.Map;
import java.util.HashMap;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InformeInventarioDiaServiceImpl implements InformeInventarioDiaService {

    private final InformeInventarioDiaRepository repository;
    private final ObjectMapper objectMapper; // ✅ ahora Spring lo inyecta automáticamente
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

        // 🔹 Rango del día
        LocalDateTime inicio = dto.fecha().atStartOfDay();
        LocalDateTime fin = dto.fecha().atTime(23, 59, 59);

        // 🔹 Materia prima del día (SALE DE InventarioServicioImpl)
        List<MateriaPrimaInventarioDTO> materiaPrimaDia =
                inventarioServicio.obtenerInventarioMateriaPrimaDia(
                        dto.sedeId(),
                        inicio,
                        fin
                );

        // 🔹 Armar JSON final del informe
        Map<String, Object> datosInforme = new HashMap<>();
        datosInforme.put("productos", dto.inventarioDia());
        datosInforme.put("materiaPrima", materiaPrimaDia);

        informe.setDatosJson(
                objectMapper.writeValueAsString(datosInforme)
        );

        return repository.save(informe);
    }


    @Override
    public List<InformeInventarioDia> obtenerInformes(Long sedeId, LocalDate fecha) {
        return repository.findBySedeIdAndFecha(sedeId, fecha);
    }
}
