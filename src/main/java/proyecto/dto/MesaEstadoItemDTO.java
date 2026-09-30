package proyecto.dto;

public record MesaEstadoItemDTO(
        InventarioDTO producto,
        String nombreLibre,
        Double precioUnitario,
        Integer cantidad,
        Double total
) {}
