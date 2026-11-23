package com.andes.bpo.ms_siniestros.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorBody(String timestamp, int status, String error, String mensaje) {}

    // 400 Bad Request: Datos inválidos, ID inexistente o campos faltantes
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorBody> manejarArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorBody(LocalDateTime.now().toString(), 400, "Solicitud inválida", ex.getMessage()));
    }

    // 409 Conflict: Reglas de negocio (ej. caso asignado a otro liquidador o sin respaldo)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorBody> manejarConflictoEstado(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody(LocalDateTime.now().toString(), 409, "Conflicto de operación", ex.getMessage()));
    }

    // 502 Bad Gateway: Falla al consultar liquidadores en ms-usuarios
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorBody> manejarErrorComunicacion(RestClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorBody(LocalDateTime.now().toString(), 502, "Error de comunicación entre microservicios",
                        "No se pudo consultar ms-usuarios: " + ex.getMessage()));
    }

    // 500 Internal Server Error: Cualquier otro error inesperado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> manejarErrorGeneral(Exception ex) {
        String detalle = ex.getMessage() != null ? ex.getMessage() : "Error inesperado";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody(LocalDateTime.now().toString(), 500, "Error interno del servidor", detalle));
    }
}