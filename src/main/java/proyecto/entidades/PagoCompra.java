package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity @Table(name = "pago_compra") @Getter @Setter @NoArgsConstructor
public class PagoCompra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "compra_id", nullable = false) private Compra compra;
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal monto;
    @Column(name = "modo_pago", nullable = false, length = 30) private String modoPago;
    @Column(length = 255) private String referencia;
    @Column(name = "registrado_por", nullable = false, length = 20) private String registradoPor;
    @Column(name = "registrado_por_id", nullable = false) private Long registradoPorId;
}
