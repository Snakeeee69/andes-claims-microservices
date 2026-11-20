package com.andes.bpo.ms_usuarios.exception;

import com.andes.bpo.ms_usuarios.dto.MensajeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<MensajeResponse> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new MensajeResponse(ex.getMessage()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<MensajeResponse> manejarReglaNegocio(ReglaNegocioException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MensajeResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MensajeResponse> manejarValidacion(MethodArgumentNotValidException ex) {
        String error = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getDefaultMessage())
                .findFirst()
                .orElse("Datos de entrada inválidos");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MensajeResponse(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MensajeResponse> manejarGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MensajeResponse("Error interno en ms-usuarios: " + ex.getMessage()));
    }
}