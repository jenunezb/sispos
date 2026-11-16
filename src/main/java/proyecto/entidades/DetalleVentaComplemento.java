package proyecto.entidades;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="detalle_venta_complemento") @Getter @Setter @NoArgsConstructor
public class DetalleVentaComplemento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="detalle_venta_id",nullable=false) private DetalleVenta detalleVenta;
 @ManyToOne @JoinColumn(name="complemento_id") private ProductoComplemento complemento;
 @Column(nullable=false,length=120) private String nombre;
 @Column(nullable=false) private Integer cantidad;
 @Column(name="precio_unitario",nullable=false) private Double precioUnitario;
 @Column(nullable=false) private Double subtotal;
}
