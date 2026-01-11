package proyecto.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class Sede {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String ubicacion;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Administrador administrador;

    @OneToMany(mappedBy = "sede")
    private List<Vendedor> vendedores;

    @OneToMany(mappedBy = "sede")
    private List<MateriaPrimaSede> materiasPrimas = new ArrayList<>();
}