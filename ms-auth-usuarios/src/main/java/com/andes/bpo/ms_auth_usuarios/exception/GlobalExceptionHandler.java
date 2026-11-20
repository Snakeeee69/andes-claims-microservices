package com.andes.bpo.ms_auth_usuarios.exception;

import com.andes.bpo.ms_auth_usuarios.dto.AuthDtos.MensajeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<MensajeResponse> manejarAuth(AuthException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new MensajeResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MensajeResponse> manejarGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MensajeResponse("Error interno en ms-auth-usuarios: " + ex.getMessage()));
    }
}