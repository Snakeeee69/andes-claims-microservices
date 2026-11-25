package com.andes.bpo.ms_auth_usuarios.controller;

import com.andes.bpo.ms_auth_usuarios.dto.AuthDtos.*;
import com.andes.bpo.ms_auth_usuarios.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private AuthService servicio;
    private AuthController controller;

    @BeforeEach
    void preparar() {
        servicio = mock(AuthService.class);
        controller = new AuthController(servicio);
    }

    @Test
    void loginDevuelveTokenYPerfil() {
        LoginRequest req =
                new LoginRequest("12345678K", "Clave1234");

        AuthResponse auth =
                new AuthResponse(
                        "token-jwt",
                        Map.of("rol", "ASEGURADO_NATURAL")
                );

        when(servicio.login(req)).thenReturn(auth);

        var respuesta = controller.login(req);

        assertEquals(HttpStatus.OK,
                respuesta.getStatusCode());
        assertEquals("token-jwt",
                respuesta.getBody().token());

        verify(servicio).login(req);
    }

    @Test
    void cambiarClaveDevuelveConfirmacion() {
        CambioClaveObligatorioRequest req =
                new CambioClaveObligatorioRequest(
                        7, "NuevaClave123"
                );

        var respuesta =
                controller.cambiarClaveObligatoria(req);

        assertEquals(HttpStatus.OK,
                respuesta.getStatusCode());

        assertEquals(
                "Contraseña actualizada correctamente",
                respuesta.getBody().mensaje()
        );

        verify(servicio).cambiarClaveObligatoria(req);
    }

    @Test
    void solicitarCodigoDevuelveConfirmacion() {
        RecuperarRequest req =
                new RecuperarRequest(
                        "12345678K", "ana@correo.cl"
                );

        when(servicio.solicitarCodigoRecuperacion(req))
                .thenReturn(
                        "Código de verificación enviado"
                );

        var respuesta = controller.solicitarCodigo(req);

        assertEquals(HttpStatus.OK,
                respuesta.getStatusCode());

        assertTrue(
                respuesta.getBody().mensaje()
                        .contains("Código generado")
        );

        verify(servicio).solicitarCodigoRecuperacion(req);
    }

    @Test
    void confirmarNuevaClaveDevuelveMensaje() {
        ConfirmarClaveRequest req =
                new ConfirmarClaveRequest(
                        "12345678K", "654321",
                        "NuevaClave123"
                );

        var respuesta =
                controller.confirmarNuevaClave(req);

        assertEquals(HttpStatus.OK,
                respuesta.getStatusCode());

        assertEquals(
                "Contraseña restablecida correctamente",
                respuesta.getBody().mensaje()
        );

        verify(servicio).confirmarNuevaClave(req);
    }

    @Test
    void registrarCredencialResponde201() {
        RegistroCredencialRequest req =
                new RegistroCredencialRequest(
                        7, "12345678K",
                        "Clave1234", false
                );

        var respuesta =
                controller.registrarCredencial(req);

        assertEquals(HttpStatus.CREATED,
                respuesta.getStatusCode());

        verify(servicio).registrarCredencialInterna(req);
    }
}
