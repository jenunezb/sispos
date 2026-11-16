package proyecto.excepciones;

public class MesaVersionConflictException extends RuntimeException {

    public MesaVersionConflictException() {
        super("La mesa fue modificada por otro usuario. Recarga su estado antes de volver a guardar.");
    }
}
