package proyecto.dto;

public record InventarioDelDia(
        Long productoId,
        String productoNombre,
        Integer stockInicial,
        Integer entradas,
        Integer ventasDelDia,
        Integer stockActual,
        Double precio,
        Double totalVendido
) {
}
