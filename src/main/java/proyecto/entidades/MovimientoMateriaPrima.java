package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Getter @Setter
public class MovimientoMateriaPrima {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "materia_prima_sede_id")
    private MateriaPrimaSede inventario;
    private LocalDateTime fecha;
    private String tipo;
    private double stockAnterior;
    private double stockNuevo;
    private Long productoId;
    private String observacion;
}
