package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "materia_prima_sede",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"materia_prima_id", "sede_id"})
        })
@Getter
@Setter
public class MateriaPrimaSede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materia_prima_id")
    private MateriaPrima materiaPrima;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id")
    private Sede sede;

    @Column(nullable = false)
    private double cantidadActualMl;

    @Column(nullable = false)
    private double mlPorVaso;

    @Column(nullable = false)
    private boolean activa = true;
}

