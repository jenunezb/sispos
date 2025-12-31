package proyecto.dto;

public record InventarioFinalDTO(
        Long sedeId,
        String productoNombre,
        Long inventarioInicial,
        Long entradas,
        Long total,
        Long inventarioFinal,
        int cantVendida,
        double precio,
        double totalVendido
) {
}
