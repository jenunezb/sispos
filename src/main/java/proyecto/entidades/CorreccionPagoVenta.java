package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Getter @Setter
@Table(name = "correccion_pago_venta")
public class CorreccionPagoVenta {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long ventaId;
    private Long cajaId;
    @Column(nullable = false) private LocalDateTime fecha;
    @Column(nullable = false) private String usuario;
    @Column(nullable = false) private String rol;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ModoPago anterior;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ModoPago nuevo;
    @Column(nullable = false, length = 500) private String motivo;
    private Double efectivoAnterior;
    private Double transferenciaAnterior;
    private Double efectivoNuevo;
    private Double transferenciaNueva;
}
