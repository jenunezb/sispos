package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"cliente_id", "producto_id"})
        }
)
public class PrecioClienteProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Double precioVenta;

    @Column(nullable = false)
    private Boolean activo = true;
}
