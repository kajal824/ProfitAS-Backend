package profitas.profitas.backend.exception;

/** Thrown when caller is not allowed to touch this resource. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
