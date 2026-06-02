package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.BalanceGeneralDTO;
import proyecto.dto.BalanceSedeDTO;
import proyecto.entidades.ModoPago;
import proyecto.entidades.Sede;
import proyecto.repositorios.DetalleVentaRepository;
import proyecto.repositorios.GastoDiarioRepository;
import proyecto.repositorios.InventarioRepository;
import proyecto.repositorios.SedeRepository;
import proyecto.repositorios.VentaRepository;
import proyecto.servicios.interfaces.BalanceServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BalanceServicioImpl implements BalanceServicio {

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final InventarioRepository inventarioRepository;
    private final SedeRepository sedeRepository;
    private final GastoDiarioRepository gastoDiarioRepository;

    @Override
    public BalanceGeneralDTO balanceDelDia(Long empresaNit) {
        LocalDateTime desde = LocalDate.now().atStartOfDay();
        LocalDateTime hasta = LocalDate.now().atTime(23, 59, 59);
        return balanceGeneral(empresaNit, desde, hasta);
    }

    @Override
    public BalanceGeneralDTO balanceGeneral(Long empresaNit, LocalDateTime desde, LocalDateTime hasta) {

        Double ventas = ventaRepository.totalVentasEntreFechasPorEmpresa(empresaNit, desde, hasta);
        Double costo = detalleVentaRepository.costoProduccionEntreFechasPorEmpresa(empresaNit, desde, hasta);
        Long cantVentas = ventaRepository.cantidadVentasEntreFechasPorEmpresa(empresaNit, desde, hasta);
        Double inventario = inventarioRepository.valorInventarioPorEmpresa(empresaNit);
        Integer stock = inventarioRepository.stockTotalPorEmpresa(empresaNit);
        Double ventasEfectivo = ventaRepository.totalVentasEntreFechasEfectivoPorEmpresa(empresaNit, desde, hasta);
        Double ventasTransferencia = ventaRepository.totalVentasEntreFechasTransferenciaPorEmpresa(empresaNit, desde, hasta);
        Double totalGastos = gastoDiarioRepository.totalGastosPorEmpresa(empresaNit, desde, hasta);
        Double gastosEfectivo = gastoDiarioRepository.totalGastosPorEmpresaYModoPago(empresaNit, ModoPago.EFECTIVO, desde, hasta);
        Double gastosTransferencia = gastoDiarioRepository.totalGastosPorEmpresaYModoPago(empresaNit, ModoPago.TRANSFERENCIA, desde, hasta);

        ventas = ventas != null ? ventas : 0.0;
        costo = costo != null ? costo : 0.0;
        cantVentas = cantVentas != null ? cantVentas : 0L;
        ventasEfectivo = ventasEfectivo != null ? ventasEfectivo : 0.0;
        ventasTransferencia = ventasTransferencia != null ? ventasTransferencia : 0.0;
        totalGastos = totalGastos != null ? totalGastos : 0.0;
        gastosEfectivo = gastosEfectivo != null ? gastosEfectivo : 0.0;
        gastosTransferencia = gastosTransferencia != null ? gastosTransferencia : 0.0;
        inventario = inventario != null ? inventario : 0.0;
        stock = stock != null ? stock : 0;
        double utilidadBruta = ventas - costo;
        double cajaEsperada = ventasEfectivo - gastosEfectivo;

        return new BalanceGeneralDTO(
                ventas,
                costo,
                utilidadBruta,
                totalGastos,
                gastosEfectivo,
                gastosTransferencia,
                cajaEsperada,
                utilidadBruta - totalGastos,
                inventario,
                stock,
                cantVentas,
                ventasEfectivo,
                ventasTransferencia
        );
    }

    @Override
    public List<BalanceSedeDTO> balancePorSede(Long empresaNit, LocalDateTime desde, LocalDateTime hasta) {

        List<Sede> sedes = sedeRepository.findByEmpresaNit(empresaNit);

        return sedes.stream().map(sede -> {

            Double ventas = ventaRepository.totalVentasPorSedeEntreFechas(sede.getId(), desde, hasta);
            Double costo = detalleVentaRepository.costoProduccionPorSedeEntreFechas(sede.getId(), desde, hasta);
            Double inventario = inventarioRepository.valorInventarioPorSede(sede.getId());
            Integer stock = inventarioRepository.stockPorSede(sede.getId());
            Long cantVentas = ventaRepository.cantidadVentasPorSedeEntreFechas(sede.getId(), desde, hasta);
            Double ventasEfectivo = ventaRepository.totalVentasEfectivoPorSedeEntreFechas(sede.getId(), desde, hasta);
            Double ventasTransferencia = ventaRepository.totalVentasTransferenciaPorSedeEntreFechas(sede.getId(), desde, hasta);
            Double totalGastos = gastoDiarioRepository.totalGastosPorSede(sede.getId(), desde, hasta);
            Double gastosEfectivo = gastoDiarioRepository.totalGastosPorSedeYModoPago(sede.getId(), ModoPago.EFECTIVO, desde, hasta);
            Double gastosTransferencia = gastoDiarioRepository.totalGastosPorSedeYModoPago(sede.getId(), ModoPago.TRANSFERENCIA, desde, hasta);

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

        }).toList();
    }

    @Override
    public List<BalanceSedeDTO> balancePorSedeHoy(Long empresaNit) {

        LocalDateTime desde = LocalDate.now().atStartOfDay();
        LocalDateTime hasta = LocalDate.now().atTime(23, 59, 59);

        return balancePorSede(empresaNit, desde, hasta);
    }
}
