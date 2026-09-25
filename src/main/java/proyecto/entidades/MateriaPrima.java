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

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "unidad_base")
    private String unidadBase = "ML";

    private String presentacion;

    @Column(name = "contenido_presentacion")
    private Double contenidoPresentacion;

    @Column(name = "precio_presentacion")
    private Double precioPresentacion;

    @Column(name = "costo_unitario")
    private Double costoUnitario;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @OneToMany(mappedBy = "materiaPrima")
    private List<MateriaPrimaSede> sedes = new ArrayList<>();

}

