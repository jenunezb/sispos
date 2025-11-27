package proyecto.entidades;

import jakarta.persistence.*;
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
    private String cedula;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String telefono;

    @JoinColumn(nullable = false)
    @ManyToOne
    private Ciudad ciudad;

    @Column(nullable = false)
    private boolean estado;

}
