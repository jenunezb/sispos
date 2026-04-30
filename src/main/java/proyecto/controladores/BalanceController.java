package proyecto.controladores;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.BalanceGeneralDTO;
import proyecto.dto.BalanceSedeDTO;
import proyecto.entidades.Administrador;
import proyecto.servicios.implementacion.AdministradorAccesoService;
import proyecto.servicios.interfaces.BalanceServicio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/administrador/balance")
@RequiredArgsConstructor
@CrossOrigin
public class BalanceController {

    private final BalanceServicio balanceServicio;
    private final AdministradorAccesoService administradorAccesoService;

    @GetMapping("/general")
    public BalanceGeneralDTO balanceGeneral(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta
    ) {

        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        Long empresaNitConsulta = administradorAccesoService.resolverEmpresaNit(admin, empresaNit);

        if (!admin.isEsSuperAdmin() && !admin.isEsAdministradorEmpresa()) {
            return consolidarBalanceGeneral(
                    obtenerBalancesPorSede(empresaNitConsulta, desde, hasta),
                    obtenerSedeIdsVisibles(admin)
            );
        }

        if (desde != null && hasta != null) {
            LocalDateTime fDesde = LocalDate.parse(desde).atStartOfDay();
            LocalDateTime fHasta = LocalDate.parse(hasta).atTime(23, 59, 59);
            return balanceServicio.balanceGeneral(empresaNitConsulta, fDesde, fHasta);
        }

        return balanceServicio.balanceDelDia(empresaNitConsulta);
    }

    @GetMapping("/sedes")
    public BalanceSedeDTO[] balancePorSede(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) Long empresaNit,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta
    ) {

        Administrador admin = administradorAccesoService.obtenerAdministradorAutenticado(authorization);
        Long empresaNitConsulta = administradorAccesoService.resolverEmpresaNit(admin, empresaNit);

        return filtrarBalances(
                obtenerBalancesPorSede(empresaNitConsulta, desde, hasta),
                obtenerSedeIdsVisibles(admin)
        ).toArray(new BalanceSedeDTO[0]);
    }

    private List<BalanceSedeDTO> obtenerBalancesPorSede(Long empresaNit, String desde, String hasta) {
        if (desde != null && hasta != null) {
            LocalDateTime fDesde = LocalDate.parse(desde).atStartOfDay();
            LocalDateTime fHasta = LocalDate.parse(hasta).atTime(23, 59, 59);
            return balanceServicio.balancePorSede(empresaNit, fDesde, fHasta);
        }

        return balanceServicio.balancePorSedeHoy(empresaNit);
    }

    private List<Long> obtenerSedeIdsVisibles(Administrador admin) {
        return administradorAccesoService.obtenerSedesVisibles(admin).stream()
                .map(sede -> sede.getId())
                .toList();
    }

    private List<BalanceSedeDTO> filtrarBalances(List<BalanceSedeDTO> balances, List<Long> sedeIdsVisibles) {
        return balances.stream()
                .filter(balance -> sedeIdsVisibles.contains(balance.sedeId()))
                .toList();
    }

    private BalanceGeneralDTO consolidarBalanceGeneral(List<BalanceSedeDTO> balances, List<Long> sedeIdsVisibles) {
        List<BalanceSedeDTO> balancesFiltrados = filtrarBalances(balances, sedeIdsVisibles);

        double totalVentas = balancesFiltrados.stream().mapToDouble(balance -> defaultDouble(balance.totalVentas())).sum();
        double costoProduccion = balancesFiltrados.stream().mapToDouble(balance -> defaultDouble(balance.costoProduccion())).sum();
        double valorInventario = balancesFiltrados.stream().mapToDouble(balance -> defaultDouble(balance.valorInventario())).sum();
        int stockTotal = balancesFiltrados.stream().mapToInt(balance -> defaultInt(balance.stockActual())).sum();
        long cantidadVentas = balancesFiltrados.stream().mapToLong(balance -> defaultLong(balance.cantidadVentas())).sum();
        double ventasEfectivo = balancesFiltrados.stream().mapToDouble(balance -> defaultDouble(balance.efectivo())).sum();
        double ventasTransferencia = balancesFiltrados.stream().mapToDouble(balance -> defaultDouble(balance.trasferencia())).sum();

        return new BalanceGeneralDTO(
                totalVentas,
                costoProduccion,
                totalVentas - costoProduccion,
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
