package proyecto.eventos;

import java.time.LocalDate;

public record EmpresaRegistradaEvent(
        Long nit,
        String nombreEmpresa,
        String nombreAdministrador,
        String correoAdministrador,
        Long celularAdministrador,
        String sedePrincipal,
        String ubicacionSede,
        LocalDate vencimientoPrueba
) {
}
