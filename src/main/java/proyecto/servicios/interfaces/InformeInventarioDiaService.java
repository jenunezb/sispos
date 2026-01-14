package proyecto.servicios.interfaces;

import com.fasterxml.jackson.core.JsonProcessingException;
import proyecto.dto.InformeInventarioDiaDTO;
import proyecto.entidades.InformeInventarioDia;

import java.time.LocalDate;
import java.util.List;

public interface InformeInventarioDiaService {
    public InformeInventarioDia guardarInforme(InformeInventarioDiaDTO dto) throws JsonProcessingException;
    public List<InformeInventarioDia> obtenerInformes(Long sedeId, LocalDate fecha);
}
