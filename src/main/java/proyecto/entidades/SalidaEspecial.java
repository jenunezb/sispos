package proyecto.entidades;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class SalidaEspecial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoSalida tipoSalida; // DAÑO, DESCUENTO, OTRO

    private Integer cantidad;
    private LocalDate fecha;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne
    @JoinColumn(name = "sede_id")
    private Sede sede;
}
