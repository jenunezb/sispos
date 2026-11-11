package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Getter @Setter
public class MovimientoStockProducto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "inventario_id") private Inventario inventario;
    private LocalDateTime fecha;
    private String tipo;
    private double stockAnterior;
    private double stockNuevo;
    private String observacion;
}
