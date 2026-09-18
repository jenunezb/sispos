package proyecto.dto;

public record RegistroEmpresaDTO(

        // Cuenta
        String correo,
        String password,

        // Administrador
        String nombre,
        String apellido,
        Long celular,

        // Empresa
        Long nit,
        String dv,
        String nombreEmpresa,

        // Sede principal
        String nombreSede,
        String ubicacionSede

) {}


