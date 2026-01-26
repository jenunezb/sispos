package proyecto.entidades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
public class TokenValidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @OneToOne
    @JoinColumn(name = "administrador_id")
    private Administrador administrador;

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;
}
