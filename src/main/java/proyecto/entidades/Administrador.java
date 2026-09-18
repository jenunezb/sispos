package proyecto.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"empresa", "sedesAsignadas"})
@Entity
public class Administrador extends Cuenta implements Serializable {

    private String nombre;

    private String apellido;

    private Long celular;

    @ManyToOne
    @JoinColumn(nullable = true)
    private Empresa empresa;

    @Column(nullable = false)
    private boolean esSuperAdmin = false;

    @Column(nullable = false)
    private boolean esAdministradorEmpresa = false;

    @ManyToMany
    @JoinTable(
            name = "administrador_sede",
            joinColumns = @JoinColumn(name = "administrador_id"),
            inverseJoinColumns = @JoinColumn(name = "sede_id")
    )
    private List<Sede> sedesAsignadas = new ArrayList<>();

}
