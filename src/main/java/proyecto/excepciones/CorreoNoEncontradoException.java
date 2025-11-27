package proyecto.excepciones;

public class CorreoNoEncontradoException extends RuntimeException {
    public CorreoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
