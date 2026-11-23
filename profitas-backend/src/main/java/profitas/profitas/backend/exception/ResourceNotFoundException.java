package profitas.profitas.backend.exception;

/** Thrown when 404 resource does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
