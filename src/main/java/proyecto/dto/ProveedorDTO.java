package proyecto.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ProveedorDTO(@NotBlank @Size(max=160) String nombre, @Size(max=30) String identificacion,
        @Size(max=160) String contacto, @Size(max=254) String correo, @Size(max=40) String telefono) {}
