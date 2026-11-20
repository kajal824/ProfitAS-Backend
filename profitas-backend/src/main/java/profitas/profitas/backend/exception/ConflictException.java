package profitas.profitas.backend.exception;

/** Thrown when the request conflicts with current state. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
