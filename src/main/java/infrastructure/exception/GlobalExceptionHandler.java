package infrastructure.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.net.URI;
import java.time.Instant;

/**
 * Global exception handler — centralizes error responses across all controllers.
 *
 * <pre>{@code
 * {
 *   "type": "https://transaction-api/errors/validation-error",
 *   "title": "Validation Error",
 *   "status": 400,
 *   "detail": "accountId: must not be blank",
 *   "instance": "/api/v1/transactions/"
 * }
 * }</pre>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation failures on request body objects annotated with {@code @Valid}.
     *
     * @param ex the validation exception
     * @return 400 Bad Request with field-level error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        log.warn("Validation error: {}", detail);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://transaction-api/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Handles constraint violations on path variables and request parameters
     * annotated with {@code @NotBlank}, {@code @NotNull}, etc.
     *
     * @param ex the constraint violation exception
     * @return 400 Bad Request
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Constraint violation");

        log.warn("Constraint violation: {}", detail);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Constraint Violation");
        problem.setType(URI.create("https://transaction-api/errors/constraint-violation"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    /**
     * Catch-all handler for any unexpected exception.
     *
     * <p>Logs the full stack trace internally but returns only a generic 500
     * message to the client — never leak internal details in production.</p>
     *
     * @param ex the unexpected exception
     * @return 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://transaction-api/errors/internal-error"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
