package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity @Table(name = "compra_detalle") @Getter @Setter @NoArgsConstructor
public class CompraDetalle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "compra_id", nullable = false) private Compra compra;
    @Column(name = "tipo_item", nullable = false, length = 20) private String tipoItem;
    @ManyToOne @JoinColumn(name = "materia_prima_id") private MateriaPrima materiaPrima;
    @ManyToOne @JoinColumn(name = "producto_id") private Producto producto;
    @Column(name = "nombre_item", nullable = false, length = 160) private String nombreItem;
    @Column(name = "unidad_base", nullable = false, length = 20) private String unidadBase;
    @Column(length = 80) private String presentacion;
    @Column(name = "contenido_presentacion", precision = 19, scale = 3) private BigDecimal contenidoPresentacion;
    @Column(nullable = false, precision = 19, scale = 3) private BigDecimal cantidad;
    @Column(name = "costo_unitario", nullable = false, precision = 19, scale = 4) private BigDecimal costoUnitario;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal subtotal;
}
