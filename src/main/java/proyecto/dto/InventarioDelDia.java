package proyecto.dto;

public record InventarioDelDia(
        Long productoId,
        String productoNombre,
        Integer stockInicial,
        Integer entradas,
        Integer salidas,
        Integer perdidas,
        Integer ventasDelDia,
        Integer stockActual,
        Double precio,
        Double totalVendido,
        Boolean stockDerivado
) {
    public InventarioDelDia(
            Long productoId,
            String productoNombre,
            Integer stockInicial,
            Integer entradas,
            Integer salidas,
            Integer perdidas,
            Integer ventasDelDia,
            Integer stockActual,
            Double precio,
            Double totalVendido
    ) {
        this(productoId, productoNombre, stockInicial, entradas, salidas, perdidas,
                ventasDelDia, stockActual, precio, totalVendido, false);
    }
}
