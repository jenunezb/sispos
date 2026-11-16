package proyecto.entidades;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="producto_complemento", uniqueConstraints=@UniqueConstraint(columnNames={"producto_id","materia_prima_id"}))
@Getter @Setter @NoArgsConstructor
public class ProductoComplemento {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) @JoinColumn(name="producto_id",nullable=false) private Producto producto;
 @ManyToOne(optional=false) @JoinColumn(name="materia_prima_id",nullable=false) private MateriaPrima materiaPrima;
 @Column(nullable=false,length=120) private String nombre;
 @Column(name="precio_adicional",nullable=false) private Double precioAdicional=0D;
 @Column(name="cantidad_consumo",nullable=false) private Double cantidadConsumo;
 @Column(nullable=false) private Boolean activo=true;
}
