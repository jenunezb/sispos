package proyecto.dto;

public interface InventarioFinalProjection {

    Long getSedeId();
    String getProductoNombre();
    Long getInventarioInicial();
    Long getEntradas();
    Long getTotal();
    Long getInventarioFinal();
    int getCantVendida();
    Double getPrecio();
    Double getTotalVendido();
}

