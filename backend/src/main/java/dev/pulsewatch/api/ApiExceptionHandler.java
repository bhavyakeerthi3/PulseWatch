package dev.pulsewatch.api;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;
@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String,String>> missing(NoSuchElementException error) { return ResponseEntity.status(404).body(Map.of("error", error.getMessage())); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String,String>> conflict(IllegalArgumentException error) { return ResponseEntity.status(409).body(Map.of("error", error.getMessage())); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,String>> invalid(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getFieldErrors().stream().map(field -> field.getField() + " " + field.getDefaultMessage()).collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    ResponseEntity<Map<String,String>> constraint() { return ResponseEntity.status(409).body(Map.of("error", "This change conflicts with an existing record. Service names must be unique.")); }
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String,String>> malformed() { return ResponseEntity.badRequest().body(Map.of("error", "Invalid request body or identifier.")); }
}
