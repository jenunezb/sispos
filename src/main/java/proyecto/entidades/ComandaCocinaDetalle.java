package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "comanda")
@Entity
@Table(name = "comanda_cocina_detalle")
public class ComandaCocinaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "comanda_id", nullable = false)
    private ComandaCocina comanda;

    @Column(name = "producto_nombre", nullable = false, length = 255)
    private String productoNombre;

    @Column(nullable = false)
    private Integer cantidad;
}
