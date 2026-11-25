package com.andes.bpo.ms_siniestros.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void argumentoInvalidoDevuelve400() {
        var r = handler.manejarArgumentoInvalido(
                new IllegalArgumentException("ID inválido")
        );

        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertEquals(400, r.getBody().status());
        assertEquals("ID inválido", r.getBody().mensaje());
        assertNotNull(r.getBody().timestamp());
    }

    @Test
    void conflictoDeEstadoDevuelve409() {
        var r = handler.manejarConflictoEstado(
                new IllegalStateException("Caso ya asignado")
        );

        assertEquals(HttpStatus.CONFLICT, r.getStatusCode());
        assertEquals("Caso ya asignado", r.getBody().mensaje());
    }

    @Test
    void falloEntreMicroserviciosDevuelve502() {
        var r = handler.manejarErrorComunicacion(
                new RestClientException("No disponible")
        );

        assertEquals(HttpStatus.BAD_GATEWAY, r.getStatusCode());
        assertTrue(r.getBody().mensaje().contains("ms-usuarios"));
    }

    @Test
    void errorInesperadoDevuelve500() {
        var r = handler.manejarErrorGeneral(
                new RuntimeException("Error inesperado")
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                r.getStatusCode());
        assertEquals("Error inesperado", r.getBody().mensaje());
    }

    @Test
    void errorSinMensajeTieneTextoPredeterminado() {
        var r = handler.manejarErrorGeneral(
                new RuntimeException()
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                r.getStatusCode());
        assertEquals("Error inesperado", r.getBody().mensaje());
    }
}
