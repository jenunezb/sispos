package proyecto.dto;

import proyecto.entidades.Cliente;

public record ClienteDTO(
        Long id,
        String nombre,
        String telefono,
        String documento,
        Boolean activo,
        String correo,
        String direccion
) {
    public static ClienteDTO desde(Cliente cliente) {
        return cliente == null ? null : new ClienteDTO(cliente.getId(), cliente.getNombre(),
                cliente.getTelefono(), cliente.getDocumento(), cliente.getActivo(),
                cliente.getCorreo(), cliente.getDireccion());
    }
}
