package proyecto.dto;

import java.util.List;

public record AdministradorEmpresaDTO(
        Integer codigo,
        String nombre,
        String apellido,
        String correo,
        Long celular,
        boolean administradorEmpresa,
        List<Long> sedeIds
) {
}
