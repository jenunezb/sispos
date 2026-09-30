package proyecto.dto;
import java.util.List;
public record ConfiguracionComplementosProductoDTO(Boolean moduloEmpresaHabilitado, Boolean habilitado, Integer gratis, List<ComplementoProductoDTO> complementos) {}
