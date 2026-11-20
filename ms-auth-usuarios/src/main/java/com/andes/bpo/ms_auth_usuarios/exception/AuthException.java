package com.andes.bpo.ms_auth_usuarios.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthException extends RuntimeException {
    private final HttpStatus status;

    public AuthException(String mensaje, HttpStatus status) {
        super(mensaje);
        this.status = status;
    }
}