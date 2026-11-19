package com.erwin.backend.exception;

import java.time.Instant;
import java.util.List;

/**
 * Formato de respuesta de error consistente para toda la API.
 * `errors` solo se llena para fallos de validación (queda null en el resto).
 */
public class ErrorResponse {

    private final String timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final List<String> errors;

    public ErrorResponse(int status, String error, String message, List<String> errors) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
        this.errors = errors;
    }

    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, null);
    }

    public String getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public List<String> getErrors() { return errors; }
}
