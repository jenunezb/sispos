package proyecto.entidades;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

@Getter @Setter @NoArgsConstructor @ToString
@Entity
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long codigo; // autoincrementable

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = true, length = 255)
    private String descripcion;

    @Column(nullable = true)
    @PositiveOrZero
    private Double precioProduccion;

    @Column(nullable = false)
    private Double precioVenta;

    @Column(length = 50)
    private String categoria;

    @Column(nullable = false)
    private Boolean estado = true;
}