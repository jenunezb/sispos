package proyecto.excepciones;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import proyecto.dto.MensajeDTO;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            CannotAcquireLockException.class,
            QueryTimeoutException.class,
            TransactionTimedOutException.class
    })
    public ResponseEntity<MensajeDTO<String>> manejarBloqueosYTimeouts(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new MensajeDTO<>(
                        true,
                        "La venta esta siendo procesada o la base de datos esta ocupada. Intenta de nuevo en unos segundos."
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MensajeDTO<String>> manejarRuntime(RuntimeException ex) {
        return ResponseEntity
                .badRequest()
                .body(new MensajeDTO<>(true, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MensajeDTO<String>> manejarExcepcionesGenerales(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new MensajeDTO<>(true, ex.getMessage()));
    }
}
