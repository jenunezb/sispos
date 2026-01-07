package proyecto.servicios.implementacion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import proyecto.dto.BalanceGeneralDTO;
import proyecto.dto.BalanceSedeDTO;
import proyecto.entidades.Sede;
import proyecto.repositorios.DetalleVentaRepository;
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

    /* ======================
       BALANCE GENERAL
       ====================== */
    @Override
    public BalanceGeneralDTO balanceDelDia() {

        LocalDateTime desde = LocalDate.now().atStartOfDay();
        LocalDateTime hasta = LocalDate.now().atTime(23, 59, 59);

        Double ventas = ventaRepository.totalVentasEntreFechas(desde, hasta);
        Double costo = detalleVentaRepository.costoProduccionEntreFechas(desde, hasta);
        Long cantVentas = ventaRepository.cantidadVentasEntreFechas(desde, hasta);
        Double ventasEfectivo = ventaRepository.totalVentasEntreFechasEfectivo(desde, hasta);
        Double ventasTransferencia = ventaRepository.totalVentasEntreFechasTransferencia(desde, hasta);

        Double inventario = inventarioRepository.valorInventario();
        Integer stock = inventarioRepository.stockTotal();

        ventas = ventas != null ? ventas : 0.0;
        costo = costo != null ? costo : 0.0;
        cantVentas = cantVentas != null ? cantVentas : 0L;
        ventasEfectivo = ventasEfectivo != null ? ventasEfectivo : 0.0;
        ventasTransferencia = ventasTransferencia != null ? ventasTransferencia : 0.0;

        return new BalanceGeneralDTO(
                ventas,
                costo,
                ventas - costo,
                inventario,
                stock,
                cantVentas,
                ventasEfectivo,
                ventasTransferencia
        );
    }


    @Override
    public List<BalanceSedeDTO> balancePorSede() {

        List<Sede> sedes = sedeRepository.findAll();

        return sedes.stream().map(sede -> {

            Double ventas = ventaRepository.totalVentasPorSede(sede.getId());
            Double costo = detalleVentaRepository.costoProduccionPorSede(sede.getId());
            Double inventario = inventarioRepository.valorInventarioPorSede(sede.getId());
            Integer stock = inventarioRepository.stockPorSede(sede.getId());
            Long cantVentas = ventaRepository.cantidadVentasPorSede(sede.getId());

            return new BalanceSedeDTO(
                    sede.getId(),
                    sede.getNombre(),
                    ventas,
                    costo,
                    ventas - costo,
                    inventario,
                    stock,
                    cantVentas
            );

        }).toList();
    }

    @Override
    public BalanceGeneralDTO balanceGeneral(LocalDateTime desde, LocalDateTime hasta) {

        Double ventas = ventaRepository.totalVentasEntreFechas(desde, hasta);
        Double costo = detalleVentaRepository.costoProduccionEntreFechas(desde, hasta);
        Long cantVentas = ventaRepository.cantidadVentasEntreFechas(desde, hasta);
        Double inventario = inventarioRepository.valorInventario();
        Integer stock = inventarioRepository.stockTotal();
        Double ventasEfectivo = ventaRepository.totalVentasEntreFechasEfectivo(desde, hasta);
        Double ventasTransferencia = ventaRepository.totalVentasEntreFechasTransferencia(desde, hasta);


        return new BalanceGeneralDTO(
                ventas,
                costo,
                ventas - costo,
                inventario,
                stock,
                cantVentas,
                ventasEfectivo,
                ventasTransferencia
        );
    }

    @Override
    public List<BalanceSedeDTO> balancePorSede(LocalDateTime desde, LocalDateTime hasta) {

        List<Sede> sedes = sedeRepository.findAll();

        return sedes.stream().map(sede -> {

            Double ventas = ventaRepository
                    .totalVentasPorSedeEntreFechas(sede.getId(), desde, hasta);

            Double costo = detalleVentaRepository
                    .costoProduccionPorSedeEntreFechas(sede.getId(), desde, hasta);

            Double inventario = inventarioRepository
                    .valorInventarioPorSede(sede.getId());

            Integer stock = inventarioRepository
                    .stockPorSede(sede.getId());

            Long cantVentas = ventaRepository.cantidadVentasPorSedeEntreFechas(sede.getId(),desde,hasta);


            return new BalanceSedeDTO(
                    sede.getId(),
                    sede.getNombre(),
                    ventas,
                    costo,
                    ventas - costo,
                    inventario,
                    stock,
                    cantVentas
            );

        }).toList();
    }

    @Override
    public List<BalanceSedeDTO> balancePorSedeHoy() {

        LocalDateTime desde = LocalDate.now().atStartOfDay();
        LocalDateTime hasta = LocalDate.now().atTime(23, 59, 59);

        List<Sede> sedes = sedeRepository.findAll();

        return sedes.stream().map(sede -> {

            Double ventas = ventaRepository.totalVentasPorSedeEntreFechas(
                    sede.getId(), desde, hasta
            );

            Double costo = detalleVentaRepository.costoProduccionPorSedeEntreFechas(
                    sede.getId(), desde, hasta
            );

            Long cantVentas = ventaRepository.cantidadVentasPorSedeEntreFechas(
                    sede.getId(), desde, hasta
            );

            Double inventario = inventarioRepository.valorInventarioPorSede(sede.getId());
            Integer stock = inventarioRepository.stockPorSede(sede.getId());

            ventas = ventas != null ? ventas : 0.0;
            costo = costo != null ? costo : 0.0;
            cantVentas = cantVentas != null ? cantVentas : 0L;

            return new BalanceSedeDTO(
                    sede.getId(),
                    sede.getNombre(),
                    ventas,
                    costo,
                    ventas - costo,
                    inventario,
                    stock,
                    cantVentas
            );

        }).toList();
    }

}
