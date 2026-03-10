package proyecto.servicios.interfaces;

import proyecto.dto.BalanceGeneralDTO;
import proyecto.dto.BalanceSedeDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface BalanceServicio {

    public BalanceGeneralDTO balanceDelDia(Long empresaNit);

    public BalanceGeneralDTO balanceGeneral(Long empresaNit, LocalDateTime desde, LocalDateTime hasta);

    List<BalanceSedeDTO> balancePorSede(Long empresaNit, LocalDateTime desde, LocalDateTime hasta);

    List<BalanceSedeDTO> balancePorSedeHoy(Long empresaNit);

}
