package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Empresa implements Serializable {

    @Id
    private Long nit;

    private String nombre;

    @OneToOne
    private Imagen logo;

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL)
    private List<Sede> sedes;

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL)
    private List<Administrador> administradores;

}
