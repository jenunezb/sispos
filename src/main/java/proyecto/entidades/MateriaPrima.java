package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "materia_prima")
@Getter
@Setter
public class MateriaPrima {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(nullable = false)
    private boolean activa = true;

    @OneToMany(mappedBy = "materiaPrima")
    private List<MateriaPrimaSede> sedes = new ArrayList<>();

}

