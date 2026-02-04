package proyecto.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class DetalleVenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;

    @ManyToOne
    @JoinColumn(name = "venta_id")
    private Venta venta;


    @ManyToOne
    @JoinColumn(
            name = "producto_id",
            referencedColumnName = "codigo",
            nullable = true
    )
    private Producto producto;



    @Column(nullable = true)
    private String nombreLibre;

}
