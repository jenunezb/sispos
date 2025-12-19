package proyecto.servicios.interfaces;

import proyecto.dto.BalanceGeneralDTO;
import proyecto.dto.BalanceSedeDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface BalanceServicio {
    /* SIN FECHAS */
    BalanceGeneralDTO balanceDelDia();
    List<BalanceSedeDTO> balancePorSede();

    /* CON FECHAS */
    BalanceGeneralDTO balanceGeneral(LocalDateTime desde, LocalDateTime hasta);
    List<BalanceSedeDTO> balancePorSede(LocalDateTime desde, LocalDateTime hasta);
    public List<BalanceSedeDTO> balancePorSedeHoy();
}
