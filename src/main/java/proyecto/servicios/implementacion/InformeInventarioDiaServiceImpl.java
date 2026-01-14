package proyecto.servicios.implementacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import proyecto.dto.InformeInventarioDiaDTO;
import proyecto.entidades.InformeInventarioDia;
import proyecto.repositorios.InformeInventarioDiaRepository;
import proyecto.servicios.interfaces.InformeInventarioDiaService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InformeInventarioDiaServiceImpl implements InformeInventarioDiaService {

    private final InformeInventarioDiaRepository repository;
    private final ObjectMapper objectMapper; // ✅ ahora Spring lo inyecta automáticamente

    @Override
    public InformeInventarioDia guardarInforme(InformeInventarioDiaDTO dto) throws JsonProcessingException {
        // ❌ Si totalVendido es 0, no guardamos nada
        if (dto.totalVendido() == 0) {
            return null; // o lanzar una excepción si quieres notificar al frontend
        }
        InformeInventarioDia informe = new InformeInventarioDia();
        informe.setSedeId(dto.sedeId());
        informe.setFecha(dto.fecha());
        informe.setTotalVendido(dto.totalVendido());

        // Convertir lista a JSON
        informe.setDatosJson(objectMapper.writeValueAsString(dto.inventarioDia()));

        return repository.save(informe);
    }

    @Override
    public List<InformeInventarioDia> obtenerInformes(Long sedeId, LocalDate fecha) {
        return repository.findBySedeIdAndFecha(sedeId, fecha);
    }
}
