package proyecto.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public class Usuario extends Cuenta implements Serializable {


    @Column(length = 10, nullable = false)
    @NotBlank(message = "La cédula es obligatoria")
    private String cedula;

    @Column(nullable = false)
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Column(nullable = false)
    @NotBlank(message = "El teléfono es obligatorio")
    private String telefono;

    @NotNull(message = "La ciudad es obligatoria")
    @JoinColumn(nullable = false)
    @ManyToOne
    private Ciudad ciudad;


    @Column(nullable = false)
    private boolean estado;

}
