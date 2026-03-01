package proyecto.controladores;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import proyecto.dto.*;
import proyecto.entidades.Administrador;
import proyecto.repositorios.AdministradorRepository;
import proyecto.servicios.interfaces.BalanceServicio;
import proyecto.utils.JWTUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/administrador/balance")
@RequiredArgsConstructor
@CrossOrigin
public class BalanceController {

    private final BalanceServicio balanceServicio;
    private final JWTUtils jwtUtils;
    private final AdministradorRepository administradorRepository;

    /* ======================
       BALANCE GENERAL
       ====================== */
    @GetMapping("/general")
    public BalanceGeneralDTO balanceGeneral(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta
    ) {

        obtenerAdminAutenticado(authorization);

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
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta
    ) {

        Administrador admin = obtenerAdminAutenticado(authorization);
        Long empresaNit = admin.getEmpresa().getNit();

        if (desde != null && hasta != null) {
            LocalDateTime fDesde = LocalDate.parse(desde).atStartOfDay();
            LocalDateTime fHasta = LocalDate.parse(hasta).atTime(23, 59, 59);
            return balanceServicio.balancePorSede(empresaNit, fDesde, fHasta)
                    .toArray(new BalanceSedeDTO[0]);
        }

        // ✅ POR DEFECTO: BALANCE DEL DÍA
        return balanceServicio.balancePorSedeHoy(empresaNit)
                .toArray(new BalanceSedeDTO[0]);
    }

    private Administrador obtenerAdminAutenticado(String authorization) {
        String token = authorization.replace("Bearer ", "");
        Jws<Claims> claims = jwtUtils.parseJwt(token);
        String correo = claims.getBody().getSubject();
        String rol = (String) claims.getBody().get("rol");

        if (!"administrador".equals(rol)) {
            throw new RuntimeException("No tiene permisos para acceder a este recurso");
        }

        return administradorRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Administrador no encontrado"));
    }

}
