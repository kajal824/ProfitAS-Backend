package profitas.profitas.backend.exception;

/** Thrown when bad credentials. */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
