package proyecto.dto;

public record AdminPinActualizarRequestDTO(
        String pinActual,
        String pinNuevo
) {
}
