package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "mesaEstado")
@Entity
@Table(name = "mesa_estado_item")
public class MesaEstadoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mesa_estado_id", nullable = false)
    private MesaEstado mesaEstado;

    @Column(name = "producto_id")
    private Long productoId;

    @Column(name = "producto_nombre", length = 255)
    private String productoNombre;

    @Column(name = "stock_actual")
    private Integer stockActual;

    @Column(name = "entradas")
    private Integer entradas;

    @Column(name = "salidas")
    private Integer salidas;

    @Column(name = "perdidas")
    private Integer perdidas;

    @Column(name = "stock_minimo")
    private Integer stockMinimo;

    @Column(name = "precio_venta")
    private Double precioVenta;

    @Column(name = "nombre_libre", length = 255)
    private String nombreLibre;

    @Column(name = "precio_unitario", nullable = false)
    private Double precioUnitario;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Double total;
}
