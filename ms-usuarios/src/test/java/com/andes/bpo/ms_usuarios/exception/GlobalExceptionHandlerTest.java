
package com.andes.bpo.ms_usuarios.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void preparar() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void usuarioNoEncontradoDevuelve404() {
        var respuesta = handler.manejarNoEncontrado(
                new RecursoNoEncontradoException("Usuario no encontrado")
        );

        assertEquals(HttpStatus.NOT_FOUND,
                respuesta.getStatusCode());

        assertEquals("Usuario no encontrado",
                respuesta.getBody().mensaje());
    }

    @Test
    void reglaNegocioDevuelve400() {
        var respuesta = handler.manejarReglaNegocio(
                new ReglaNegocioException("RUT duplicado")
        );

        assertEquals(HttpStatus.BAD_REQUEST,
                respuesta.getStatusCode());

        assertEquals("RUT duplicado",
                respuesta.getBody().mensaje());
    }

    @Test
    void errorGeneralDevuelve500() {
        var respuesta = handler.manejarGeneral(
                new RuntimeException("Error inesperado")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                respuesta.getStatusCode());

        assertTrue(respuesta.getBody().mensaje()
                .contains("Error inesperado"));
    }

    @Test
    void validacionDevuelvePrimerError() {
        MethodArgumentNotValidException excepcion =
                mock(MethodArgumentNotValidException.class);

        BindingResult resultado = mock(BindingResult.class);

        when(excepcion.getBindingResult()).thenReturn(resultado);

        when(resultado.getFieldErrors()).thenReturn(
                List.of(new FieldError(
                        "usuario", "correo", "Correo obligatorio"
                ))
        );

        var respuesta = handler.manejarValidacion(excepcion);

        assertEquals(HttpStatus.BAD_REQUEST,
                respuesta.getStatusCode());

        assertEquals("Correo obligatorio",
                respuesta.getBody().mensaje());
    }

    @Test
    void validacionSinErroresDevuelveMensajeGenerico() {
        MethodArgumentNotValidException excepcion =
                mock(MethodArgumentNotValidException.class);

        BindingResult resultado = mock(BindingResult.class);

        when(excepcion.getBindingResult()).thenReturn(resultado);
        when(resultado.getFieldErrors()).thenReturn(List.of());

        var respuesta = handler.manejarValidacion(excepcion);

        assertEquals("Datos de entrada inválidos",
                respuesta.getBody().mensaje());
    }
}
