package proyecto.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class Producto implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @PositiveOrZero
    private Double precioProduccion;

    @Column(nullable = false)
    private Double precioVenta;

    @Column(length = 50)
    private String categoria;

    @Column(name = "codigo_estandar_fiscal", length = 50)
    private String codigoEstandarFiscal;

    @Column(name = "unidad_medida_dian", length = 10)
    private String unidadMedidaDian;

    @Column(name = "tributo_codigo", length = 10)
    private String tributoCodigo;

    @Column(name = "tarifa_iva", precision = 9, scale = 6)
    private BigDecimal tarifaIva;

    @Column(name = "tarifa_inc", precision = 9, scale = 6)
    private BigDecimal tarifaInc;

    @Column(name = "tarifa_ica", precision = 9, scale = 6)
    private BigDecimal tarifaIca;

    @Column(name = "precio_incluye_impuestos")
    private Boolean precioIncluyeImpuestos;

    @Column(nullable = false)
    private Boolean estado = true;

    private Boolean activo = true;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoMateriaPrima> materiasPrimas = new ArrayList<>();

}
