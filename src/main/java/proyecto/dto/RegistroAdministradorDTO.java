package proyecto.dto;

public record RegistroAdministradorDTO(

        // Cuenta
        String correo,
        String password,

        // Administrador
        String nombre,
        String apellido,
        Long celular,

        // Empresa
        Long nit,
        String nombreEmpresa,
        Long logoId

) {}

