package proyecto.entidades;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class DetalleVenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;

    @Column(name = "precio_unitario_fiscal", precision = 19, scale = 6)
    private BigDecimal precioUnitarioFiscal;

    @Column(name = "subtotal_fiscal", precision = 19, scale = 6)
    private BigDecimal subtotalFiscal;

    @Column(name = "descuento_fiscal", precision = 19, scale = 6)
    private BigDecimal descuentoFiscal;

    @Column(name = "base_impuesto_fiscal", precision = 19, scale = 6)
    private BigDecimal baseImpuestoFiscal;

    @Column(name = "tarifa_impuesto_fiscal", precision = 9, scale = 6)
    private BigDecimal tarifaImpuestoFiscal;

    @Column(name = "valor_impuesto_fiscal", precision = 19, scale = 6)
    private BigDecimal valorImpuestoFiscal;

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
