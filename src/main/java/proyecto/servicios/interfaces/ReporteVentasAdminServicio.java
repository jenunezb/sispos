package proyecto.servicios.interfaces;

import proyecto.dto.ComparativoVentasMensualDTO;
import proyecto.dto.CrecimientoVentasMensualDTO;
import proyecto.dto.VentasPorDiaDTO;
import proyecto.dto.VentasPorHoraDTO;
import proyecto.dto.VentasPorMesDTO;

import java.time.LocalDate;
import java.util.List;

public interface ReporteVentasAdminServicio {

    List<VentasPorMesDTO> obtenerVentasPorMes(List<Long> sedeIds, LocalDate desde, LocalDate hasta);

    List<VentasPorDiaDTO> obtenerVentasPorDia(List<Long> sedeIds, LocalDate desde, LocalDate hasta);

    List<VentasPorHoraDTO> obtenerVentasPorHora(List<Long> sedeIds, LocalDate desde, LocalDate hasta);

    ComparativoVentasMensualDTO obtenerComparativoMensual(List<Long> sedeIds, Integer anio, Integer mes);

    List<CrecimientoVentasMensualDTO> obtenerCrecimientoMensual(List<Long> sedeIds, LocalDate desde, LocalDate hasta);
}
