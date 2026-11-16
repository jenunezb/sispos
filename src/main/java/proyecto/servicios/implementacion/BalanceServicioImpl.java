package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.BalanceGeneralDTO;
import proyecto.dto.BalanceSedeDTO;
import proyecto.entidades.ModoPago;
import proyecto.entidades.EstadoCaja;
import proyecto.entidades.Sede;
import proyecto.repositorios.DetalleVentaRepository;
import proyecto.repositorios.GastoDiarioRepository;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VentaRepository;
import proyecto.repositorios.CajaTurnoRepository;
import proyecto.servicios.interfaces.BalanceServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import proyecto.utils.FechaColombiaUtils;

@Service
@RequiredArgsConstructor
public class BalanceServicioImpl implements BalanceServicio {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final InventarioRepository inventarioRepository;
    private final SedeRepository sedeRepository;
    private final GastoDiarioRepository gastoDiarioRepository;
    private final SuscripcionFeatureService suscripcionFeatureService;
    private final CajaTurnoRepository cajaTurnoRepository;

    @Override
    public BalanceGeneralDTO balanceDelDia(Long empresaNit) {
        return consolidarBalanceGeneral(balancePorSedeHoy(empresaNit));
    }

    @Override
    public BalanceGeneralDTO balanceGeneral(Long empresaNit, LocalDateTime desde, LocalDateTime hasta) {
        return consolidarBalanceGeneral(balancePorSede(empresaNit, desde, hasta));
    }

    @Override
    public List<BalanceSedeDTO> balancePorSede(Long empresaNit, LocalDateTime desde, LocalDateTime hasta) {

        List<Sede> sedes = sedeRepository.findByEmpresaNit(empresaNit);

        return sedes.stream().map(sede -> construirBalanceSede(sede, desde, hasta)).toList();
    }

    private BalanceSedeDTO construirBalanceSede(Sede sede, LocalDateTime desde, LocalDateTime hasta) {

            Double ventas = ventaRepository.totalVentasPorSedeEntreFechas(sede.getId(), desde, hasta);
            Double costo = detalleVentaRepository.costoProduccionPorSedeEntreFechas(sede.getId(), desde, hasta);
            Double inventario = inventarioRepository.valorInventarioPorSede(sede.getId());
            Integer stock = inventarioRepository.stockPorSede(sede.getId());
            Long cantVentas = ventaRepository.cantidadVentasPorSedeEntreFechas(sede.getId(), desde, hasta);
            Double ventasEfectivo = ventaRepository.totalVentasEfectivoPorSedeEntreFechas(sede.getId(), desde, hasta);
            Double ventasTransferencia = ventaRepository.totalVentasTransferenciaPorSedeEntreFechas(sede.getId(), desde, hasta);
            boolean gastosHabilitados = suscripcionFeatureService.tieneGastosHabilitados(sede.getId());
            Double totalGastos = gastosHabilitados ? gastoDiarioRepository.totalGastosPorSede(sede.getId(), desde, hasta) : 0.0;
            Double gastosEfectivo = gastosHabilitados ? gastoDiarioRepository.totalGastosPorSedeYModoPago(sede.getId(), ModoPago.EFECTIVO, desde, hasta) : 0.0;
            Double gastosTransferencia = gastosHabilitados ? gastoDiarioRepository.totalGastosPorSedeYModoPago(sede.getId(), ModoPago.TRANSFERENCIA, desde, hasta) : 0.0;

            ventas = ventas != null ? ventas : 0.0;
            costo = costo != null ? costo : 0.0;
            inventario = inventario != null ? inventario : 0.0;
            stock = stock != null ? stock : 0;
            cantVentas = cantVentas != null ? cantVentas : 0L;
            ventasEfectivo = ventasEfectivo != null ? ventasEfectivo : 0.0;
            ventasTransferencia = ventasTransferencia != null ? ventasTransferencia : 0.0;
            totalGastos = totalGastos != null ? totalGastos : 0.0;
            gastosEfectivo = gastosEfectivo != null ? gastosEfectivo : 0.0;
            gastosTransferencia = gastosTransferencia != null ? gastosTransferencia : 0.0;
            double utilidadBruta = ventas - costo;

            return new BalanceSedeDTO(
                    sede.getId(),
                    sede.getUbicacion(),
                    ventas,
                    ventasEfectivo,
                    ventasTransferencia,
                    costo,
                    utilidadBruta,
                    totalGastos,
                    gastosEfectivo,
                    gastosTransferencia,
                    ventasEfectivo - gastosEfectivo,
                    utilidadBruta - totalGastos,
                    inventario,
                    stock,
                    cantVentas
            );
    }

    @Override
    public List<BalanceSedeDTO> balancePorSedeHoy(Long empresaNit) {
        LocalDateTime ahora = FechaColombiaUtils.ahora();
        LocalDateTime inicioDia = FechaColombiaUtils.hoy().atStartOfDay();
        return sedeRepository.findByEmpresaNit(empresaNit).stream()
                .map(sede -> {
                    LocalDateTime desde = cajaTurnoRepository
                            .findFirstBySedeIdAndEstadoOrderByFechaAperturaDesc(sede.getId(), EstadoCaja.ABIERTA)
                            .map(caja -> caja.getFechaApertura())
                            .orElse(inicioDia);
                    return construirBalanceSede(sede, desde, ahora);
                })
                .toList();
    }

    private BalanceGeneralDTO consolidarBalanceGeneral(List<BalanceSedeDTO> balances) {
        double totalVentas = balances.stream().mapToDouble(balance -> defaultDouble(balance.totalVentas())).sum();
        double costoProduccion = balances.stream().mapToDouble(balance -> defaultDouble(balance.costoProduccion())).sum();
        double totalGastos = balances.stream().mapToDouble(balance -> defaultDouble(balance.totalGastos())).sum();
        double gastosEfectivo = balances.stream().mapToDouble(balance -> defaultDouble(balance.gastosEfectivo())).sum();
        double gastosTransferencia = balances.stream().mapToDouble(balance -> defaultDouble(balance.gastosTransferencia())).sum();
        double valorInventario = balances.stream().mapToDouble(balance -> defaultDouble(balance.valorInventario())).sum();
        int stockTotal = balances.stream().mapToInt(balance -> defaultInt(balance.stockActual())).sum();
        long cantidadVentas = balances.stream().mapToLong(balance -> defaultLong(balance.cantidadVentas())).sum();
        double ventasEfectivo = balances.stream().mapToDouble(balance -> defaultDouble(balance.efectivo())).sum();
        double ventasTransferencia = balances.stream().mapToDouble(balance -> defaultDouble(balance.trasferencia())).sum();

        double utilidadBruta = totalVentas - costoProduccion;

        return new BalanceGeneralDTO(
                totalVentas,
                costoProduccion,
                utilidadBruta,
                totalGastos,
                gastosEfectivo,
                gastosTransferencia,
                ventasEfectivo - gastosEfectivo,
                utilidadBruta - totalGastos,
                valorInventario,
                stockTotal,
                cantidadVentas,
                ventasEfectivo,
                ventasTransferencia
        );
    }

    private double defaultDouble(Double valor) {
        return valor != null ? valor : 0.0;
    }

    private int defaultInt(Integer valor) {
        return valor != null ? valor : 0;
    }

    private long defaultLong(Long valor) {
        return valor != null ? valor : 0L;
    }
}
