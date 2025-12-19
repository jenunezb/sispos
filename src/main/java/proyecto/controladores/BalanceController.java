package proyecto.controladores;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
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

        /* ======================
           BALANCE GENERAL
           ====================== */
        @GetMapping("/general")
        public BalanceGeneralDTO balanceGeneral(
                @RequestParam(required = false) String desde,
                @RequestParam(required = false) String hasta
        ) {

            if (desde != null && hasta != null) {
                LocalDateTime fDesde = LocalDate.parse(desde).atStartOfDay();
                LocalDateTime fHasta = LocalDate.parse(hasta).atTime(23, 59, 59);
                return balanceServicio.balanceGeneral(fDesde, fHasta);
            }

            return balanceServicio.balanceDelDia();
        }

    /* ======================
BALANCE POR SEDES
====================== */
    @GetMapping("/sedes")
    public BalanceSedeDTO[] balancePorSede(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta
    ) {

        if (desde != null && hasta != null) {
            LocalDateTime fDesde = LocalDate.parse(desde).atStartOfDay();
            LocalDateTime fHasta = LocalDate.parse(hasta).atTime(23, 59, 59);
            return balanceServicio.balancePorSede(fDesde, fHasta)
                    .toArray(new BalanceSedeDTO[0]);
        }

        // ✅ POR DEFECTO: BALANCE DEL DÍA
        return balanceServicio.balancePorSedeHoy()
                .toArray(new BalanceSedeDTO[0]);
    }


}

