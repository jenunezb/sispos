package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "producto_materia_prima",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"producto_id", "materia_prima_id"})
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

    @Column(name = "ml_consumidos", nullable = false)
    private double mlConsumidos;
}
