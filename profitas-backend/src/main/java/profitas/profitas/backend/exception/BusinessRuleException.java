package profitas.profitas.backend.exception;

/** Thrown when a business rule rejected the request. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
