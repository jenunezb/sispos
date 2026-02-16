package proyecto.entidades;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
public class Administrador extends Cuenta implements Serializable {

    private String nombre;

    private String apellido;

    private Long celular;

}
