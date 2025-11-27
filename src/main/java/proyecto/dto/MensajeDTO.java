package proyecto.dto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class MensajeDTO<T> {
    private boolean error;
    private T respuesta;
}
