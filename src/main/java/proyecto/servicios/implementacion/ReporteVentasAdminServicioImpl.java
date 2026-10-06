package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.ComparativoVentasMensualDTO;
import proyecto.dto.CrecimientoVentasMensualDTO;
import proyecto.dto.VentaDiaResumenProjection;
import proyecto.dto.VentaHoraResumenProjection;
import proyecto.dto.VentaMesResumenProjection;
import proyecto.dto.VentasPorDiaDTO;
import proyecto.dto.VentasPorHoraDTO;
import proyecto.dto.VentasPorMesDTO;
import proyecto.repositorios.VentaRepository;
import proyecto.servicios.interfaces.ReporteVentasAdminServicio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteVentasAdminServicioImpl implements ReporteVentasAdminServicio {

    private static final Locale LOCALE_ES = Locale.forLanguageTag("es-CO");
    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    private final VentaRepository ventaRepository;

    @Override
    public List<VentasPorMesDTO> obtenerVentasPorMes(List<Long> sedeIds, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        if (sedeIds.isEmpty()) {
            return List.of();
        }

        Map<YearMonth, VentaMesResumenProjection> resumenPorMes = ventaRepository
                .resumenVentasPorMes(sedeIds, inicioDelDia(desde), finDelDia(hasta))
                .stream()
                .collect(Collectors.toMap(
                        item -> YearMonth.of(item.getAnio(), item.getMes()),
                        Function.identity()
                ));

        List<VentasPorMesDTO> respuesta = new ArrayList<>();
        YearMonth actual = YearMonth.from(desde);
        YearMonth fin = YearMonth.from(hasta);

        while (!actual.isAfter(fin)) {
            VentaMesResumenProjection resumen = resumenPorMes.get(actual);
            respuesta.add(new VentasPorMesDTO(
                    actual.getYear(),
                    actual.getMonthValue(),
                    formatearPeriodo(actual),
                    resumen != null ? defaultDouble(resumen.getTotalVentas()) : 0.0,
                    resumen != null ? defaultLong(resumen.getCantidadVentas()) : 0L
            ));
            actual = actual.plusMonths(1);
        }

        return respuesta;
    }

    @Override
    public List<VentasPorDiaDTO> obtenerVentasPorDia(List<Long> sedeIds, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        if (sedeIds.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, VentaDiaResumenProjection> resumenPorDia = ventaRepository
                .resumenVentasPorDia(sedeIds, inicioDelDia(desde), finDelDia(hasta))
                .stream()
                .collect(Collectors.toMap(
                        item -> LocalDate.of(item.getAnio(), item.getMes(), item.getDia()),
                        Function.identity()
                ));

        List<VentasPorDiaDTO> respuesta = new ArrayList<>();
        LocalDate actual = desde;

        while (!actual.isAfter(hasta)) {
            VentaDiaResumenProjection resumen = resumenPorDia.get(actual);
            respuesta.add(new VentasPorDiaDTO(
                    actual,
                    formatearDiaSemana(actual.getDayOfWeek()),
                    resumen != null ? defaultDouble(resumen.getTotalVentas()) : 0.0,
                    resumen != null ? defaultLong(resumen.getCantidadVentas()) : 0L
            ));
            actual = actual.plusDays(1);
        }

        return respuesta;
    }

    @Override
    public List<VentasPorHoraDTO> obtenerVentasPorHora(List<Long> sedeIds, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        if (sedeIds.isEmpty()) {
            return List.of();
        }

        Map<Integer, VentaHoraResumenProjection> resumenPorHora = ventaRepository
                .resumenVentasPorHora(sedeIds, inicioDelDia(desde), finDelDia(hasta))
                .stream()
                .collect(Collectors.toMap(VentaHoraResumenProjection::getHora, Function.identity()));

        List<VentasPorHoraDTO> respuesta = new ArrayList<>();
        for (int hora = 0; hora < 24; hora++) {
            VentaHoraResumenProjection resumen = resumenPorHora.get(hora);
            respuesta.add(new VentasPorHoraDTO(
                    hora,
                    String.format("%02d:00", hora),
                    resumen != null ? defaultDouble(resumen.getTotalVentas()) : 0.0,
                    resumen != null ? defaultLong(resumen.getCantidadVentas()) : 0L
            ));
        }

        return respuesta;
    }

    @Override
    public ComparativoVentasMensualDTO obtenerComparativoMensual(List<Long> sedeIds, Integer anio, Integer mes) {
        if (sedeIds.isEmpty()) {
            return new ComparativoVentasMensualDTO(anio, mes, null, 0.0, null, 0.0, 0.0, 0.0);
        }

        YearMonth periodoActual = resolverPeriodoComparativo(anio, mes);
        YearMonth periodoAnterior = periodoActual.minusYears(1);

        double totalActual = defaultDouble(ventaRepository.totalVentasPorSedesEntreFechas(
                sedeIds,
                periodoActual.atDay(1).atStartOfDay(),
                periodoActual.atEndOfMonth().atTime(23, 59, 59)
        ));
        double totalAnterior = defaultDouble(ventaRepository.totalVentasPorSedesEntreFechas(
                sedeIds,
                periodoAnterior.atDay(1).atStartOfDay(),
                periodoAnterior.atEndOfMonth().atTime(23, 59, 59)
        ));

        return new ComparativoVentasMensualDTO(
                periodoActual.getYear(),
                periodoActual.getMonthValue(),
                formatearPeriodo(periodoActual),
                totalActual,
                formatearPeriodo(periodoAnterior),
                totalAnterior,
                totalActual - totalAnterior,
                calcularCrecimiento(totalActual, totalAnterior)
        );
    }

    @Override
    public List<CrecimientoVentasMensualDTO> obtenerCrecimientoMensual(List<Long> sedeIds, LocalDate desde, LocalDate hasta) {
        List<VentasPorMesDTO> ventasPorMes = obtenerVentasPorMes(sedeIds, desde, hasta);
        List<CrecimientoVentasMensualDTO> respuesta = new ArrayList<>();

        Double totalMesAnterior = null;
        for (VentasPorMesDTO ventaMes : ventasPorMes) {
            respuesta.add(new CrecimientoVentasMensualDTO(
                    ventaMes.anio(),
                    ventaMes.mes(),
                    ventaMes.periodo(),
                    ventaMes.totalVentas(),
                    totalMesAnterior,
                    totalMesAnterior == null ? null : calcularCrecimiento(ventaMes.totalVentas(), totalMesAnterior)
            ));
            totalMesAnterior = ventaMes.totalVentas();
        }

        return respuesta;
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new RuntimeException("Los parametros desde y hasta son obligatorios");
        }
        if (hasta.isBefore(desde)) {
            throw new RuntimeException("La fecha hasta no puede ser menor que desde");
        }
    }

    private LocalDateTime inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay();
    }

    private LocalDateTime finDelDia(LocalDate fecha) {
        return fecha.atTime(23, 59, 59);
    }

    private YearMonth resolverPeriodoComparativo(Integer anio, Integer mes) {
        LocalDate hoy = LocalDate.now(ZONA_COLOMBIA);
        int anioResuelto = anio != null ? anio : hoy.getYear();
        int mesResuelto = mes != null ? mes : hoy.getMonthValue();
        return YearMonth.of(anioResuelto, mesResuelto);
    }

    private String formatearPeriodo(YearMonth periodo) {
        String mes = periodo.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES);
        return mes.substring(0, 1).toUpperCase(LOCALE_ES) + mes.substring(1) + " " + periodo.getYear();
    }

    private String formatearDiaSemana(DayOfWeek dia) {
        String nombre = dia.getDisplayName(TextStyle.FULL, LOCALE_ES);
        return nombre.substring(0, 1).toUpperCase(LOCALE_ES) + nombre.substring(1);
    }

    private Double calcularCrecimiento(Double valorActual, Double valorBase) {
        if (valorBase == null) {
            return null;
        }
        if (Math.abs(valorBase) < 0.000001d) {
            return Math.abs(valorActual) < 0.000001d ? 0.0 : null;
        }
        return ((valorActual - valorBase) / valorBase) * 100.0;
    }

    private double defaultDouble(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private long defaultLong(Long valor) {
        return valor != null ? valor : 0L;
    }
}
