package proyecto.dto;

public record MateriaPrimaInventarioDTO(
        Long codigo,
        String nombre,
        double stockInicial,
        double entradas,
        double salidas,
        double perdidas,
        double vendidas,
        double stockActual
) {
}
