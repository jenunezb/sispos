package proyecto.dto;

public record InventarioDTO(
        Long id,
        Long productoId,
        String productoNombre,
        Integer stockActual,
        Integer entradas,
        Integer salidas,
        Integer perdidas,
        Integer stockMinimo,
        Double precioVenta, String codigoBarras

) {
    public InventarioDTO(Long id, Long productoId, String productoNombre, Integer stockActual,
                         Integer entradas, Integer salidas, Integer perdidas, Integer stockMinimo,
                         Double precioVenta) {
        this(id, productoId, productoNombre, stockActual, entradas, salidas, perdidas, stockMinimo, precioVenta, null);
    }
}

