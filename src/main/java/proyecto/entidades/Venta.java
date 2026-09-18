package proyecto.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_consecutivo", nullable = false)
    private Long numeroConsecutivo;

    private LocalDateTime fecha;
    private Double total;

    @Column(name = "moneda_codigo", length = 3)
    private String monedaCodigo;

    @Column(name = "forma_pago_dian", length = 10)
    private String formaPagoDian;

    @Column(name = "medio_pago_dian", length = 10)
    private String medioPagoDian;

    @Column(name = "fecha_vencimiento_pago")
    private LocalDateTime fechaVencimientoPago;

    @Column(name = "subtotal_fiscal", precision = 19, scale = 6)
    private BigDecimal subtotalFiscal;

    @Column(name = "impuestos_fiscal", precision = 19, scale = 6)
    private BigDecimal impuestosFiscal;

    @Column(name = "descuentos_fiscal", precision = 19, scale = 6)
    private BigDecimal descuentosFiscal;

    @Column(name = "total_fiscal", precision = 19, scale = 6)
    private BigDecimal totalFiscal;

    // Puede vender un vendedor
    @ManyToOne
    @JoinColumn(name = "vendedor_id", nullable = true)
    private Vendedor vendedor;

    // O puede vender un administrador
    @ManyToOne
    @JoinColumn(name = "administrador_id", nullable = true)
    private Administrador administrador;

    @ManyToOne
    @JoinColumn(name = "sede_id")
    private Sede sede;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL)
    private List<DetalleVenta> detalles;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_pago", nullable = false)
    private ModoPago modoPago;

    @Column(name = "monto_efectivo")
    private Double montoEfectivo;

    @Column(name = "monto_transferencia")
    private Double montoTransferencia;

    @Column(name = "es_domicilio", nullable = false)
    private Boolean esDomicilio = false;

    @Column(name = "direccion_domicilio", length = 500)
    private String direccionDomicilio;

    @Column(name = "costo_domicilio")
    private Double costoDomicilio;

    @Column(name = "nombre_recibe_domicilio", length = 255)
    private String nombreRecibeDomicilio;

    @Column(name = "celular_recibe_domicilio", length = 50)
    private String celularRecibeDomicilio;

    @Column(nullable = false)
    private Boolean anulado = false;
}
