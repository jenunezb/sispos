package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "producto_materia_prima",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"producto_id", "materia_prima_sede_id"})
        }
)
@Getter
@Setter
public class ProductoMateriaPrima {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materia_prima_id")
    private MateriaPrima materiaPrima;

    @ManyToOne(optional = false)
    @JoinColumn(name = "materia_prima_sede_id")
    private MateriaPrimaSede materiaPrimaSede;

    @Column(name = "ml_consumidos", nullable = false)
    private double mlConsumidos;
}
