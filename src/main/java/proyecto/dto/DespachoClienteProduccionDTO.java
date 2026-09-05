package proyecto.dto;

public record DespachoClienteProduccionDTO(
        Long clienteId,
        String clienteNombre,
        Integer totalUnidades
) {
}
