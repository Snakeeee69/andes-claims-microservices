package com.andes.bpo.ms_auth_usuarios.service;

import com.andes.bpo.ms_auth_usuarios.dto.AuthDtos.*;
import com.andes.bpo.ms_auth_usuarios.exception.AuthException;
import com.andes.bpo.ms_auth_usuarios.model.Credencial;
import com.andes.bpo.ms_auth_usuarios.repository.CredencialRepository;
import com.andes.bpo.ms_auth_usuarios.security.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private CredencialRepository repositorio;
    private PasswordEncoder encoder;
    private JwtUtil jwtUtil;
    private JavaMailSender mailSender;
    private AuthService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(CredencialRepository.class);
        encoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);
        mailSender = mock(JavaMailSender.class);

        servicio = new AuthService(
                repositorio, encoder, jwtUtil, mailSender
        );
    }

    private Credencial credencialDePrueba() {
        return Credencial.builder()
                .idUsuario(10)
                .rut("12345678K")
                .passwordHash("hash-original")
                .requiereCambioClave(false)
                .build();
    }

    @Test
    void normalizarRutConFormato() {
        assertEquals("12345678K",
                servicio.normalizarRut("12.345.678-k"));
    }

    @Test
    void normalizarRutNulo() {
        assertEquals("", servicio.normalizarRut(null));
    }

    @Test
    void loginConRutInexistente() {
        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.empty());

        assertThrows(AuthException.class,
                () -> servicio.login(
                        new LoginRequest("12.345.678-k", "Clave123")
                ));
    }

    @Test
    void loginConClaveIncorrecta() {
        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.of(credencialDePrueba()));

        when(encoder.matches("Incorrecta", "hash-original"))
                .thenReturn(false);

        assertThrows(AuthException.class,
                () -> servicio.login(
                        new LoginRequest("12345678K", "Incorrecta")
                ));
    }

    @Test
    void registrarCredencialDuplicada() {
        when(repositorio.existsByRut("12345678K"))
                .thenReturn(true);

        RegistroCredencialRequest request =
                new RegistroCredencialRequest(
                        10, "12.345.678-k", "Clave123", false
                );

        assertThrows(AuthException.class,
                () -> servicio.registrarCredencialInterna(request));

        verify(repositorio, never()).save(any());
    }

    @Test
    void registrarCredencialNueva() {
        when(repositorio.existsByRut("12345678K"))
                .thenReturn(false);

        when(encoder.encode("Clave123"))
                .thenReturn("hash-seguro");

        RegistroCredencialRequest request =
                new RegistroCredencialRequest(
                        10, "12.345.678-k", "Clave123", false
                );

        servicio.registrarCredencialInterna(request);

        verify(repositorio).save(argThat(c ->
                "12345678K".equals(c.getRut()) &&
                "hash-seguro".equals(c.getPasswordHash()) &&
                Boolean.FALSE.equals(c.getRequiereCambioClave()) &&
                Integer.valueOf(10).equals(c.getIdUsuario())
        ));
    }

    @Test
    void registrarSinClaveUsaClaveProvisoria() {
        when(encoder.encode("123456"))
                .thenReturn("hash-provisorio");

        RegistroCredencialRequest request =
                new RegistroCredencialRequest(
                        10, "12345678K", "", false
                );

        servicio.registrarCredencialInterna(request);

        verify(repositorio).save(argThat(c ->
                "hash-provisorio".equals(c.getPasswordHash()) &&
                Boolean.TRUE.equals(c.getRequiereCambioClave())
        ));
    }

    @Test
    void registrarConCambioObligatorio() {
        when(encoder.encode("Clave123"))
                .thenReturn("hash-seguro");

        servicio.registrarCredencialInterna(
                new RegistroCredencialRequest(
                        10, "12345678K", "Clave123", true
                )
        );

        verify(repositorio).save(argThat(c ->
                Boolean.TRUE.equals(c.getRequiereCambioClave())
        ));
    }

    @Test
    void rechazarClaveProvisoriaReutilizada() {
        assertThrows(AuthException.class,
                () -> servicio.cambiarClaveObligatoria(
                        new CambioClaveObligatorioRequest(
                                10, "123456"
                        )
                ));

        verify(repositorio, never()).save(any());
    }

    @Test
    void cambiarClaveDeUsuarioInexistente() {
        when(repositorio.findByIdUsuario(10))
                .thenReturn(Optional.empty());

        assertThrows(AuthException.class,
                () -> servicio.cambiarClaveObligatoria(
                        new CambioClaveObligatorioRequest(
                                10, "NuevaClave123"
                        )
                ));
    }

    @Test
    void cambiarClaveCorrectamente() {
        Credencial credencial = credencialDePrueba();
        credencial.setRequiereCambioClave(true);

        when(repositorio.findByIdUsuario(10))
                .thenReturn(Optional.of(credencial));

        when(encoder.encode("NuevaClave123"))
                .thenReturn("nuevo-hash");

        servicio.cambiarClaveObligatoria(
                new CambioClaveObligatorioRequest(
                        10, "NuevaClave123"
                )
        );

        assertEquals("nuevo-hash",
                credencial.getPasswordHash());

        assertFalse(credencial.getRequiereCambioClave());

        verify(repositorio).save(credencial);
    }

    @Test
    void recuperarClaveConRutInexistente() {
        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.empty());

        assertThrows(AuthException.class,
                () -> servicio.solicitarCodigoRecuperacion(
                        new RecuperarRequest(
                                "12.345.678-k",
                                "usuario@ejemplo.com"
                        )
                ));
    }

    @Test
    void confirmarCodigoConRutInexistente() {
        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.empty());

        assertThrows(AuthException.class,
                () -> servicio.confirmarNuevaClave(
                        new ConfirmarClaveRequest(
                                "12345678K", "654321", "ClaveNueva123"
                        )
                ));
    }

    @Test
    void confirmarCodigoNoGenerado() {
        Credencial credencial = credencialDePrueba();

        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.of(credencial));

        assertThrows(AuthException.class,
                () -> servicio.confirmarNuevaClave(
                        new ConfirmarClaveRequest(
                                "12345678K", "654321", "ClaveNueva123"
                        )
                ));

        verify(repositorio, never()).save(any());
    }

    @Test
    void confirmarCodigoIncorrecto() {
        Credencial credencial = credencialDePrueba();
        credencial.setCodigoRecuperacionHash("hash-codigo");

        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.of(credencial));

        when(encoder.matches("999999", "hash-codigo"))
                .thenReturn(false);

        assertThrows(AuthException.class,
                () -> servicio.confirmarNuevaClave(
                        new ConfirmarClaveRequest(
                                "12345678K", "999999", "ClaveNueva123"
                        )
                ));

        verify(repositorio, never()).save(any());
    }

    @Test
    void confirmarCodigoCorrectoCambiaClave() {
        Credencial credencial = credencialDePrueba();
        credencial.setCodigoRecuperacionHash("hash-codigo");

        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.of(credencial));

        when(encoder.matches("654321", "hash-codigo"))
                .thenReturn(true);

        when(encoder.encode("ClaveNueva123"))
                .thenReturn("hash-nuevo");

        servicio.confirmarNuevaClave(
                new ConfirmarClaveRequest(
                        "12345678K", "654321", "ClaveNueva123"
                )
        );

        assertEquals("hash-nuevo",
                credencial.getPasswordHash());

        assertNull(credencial.getCodigoRecuperacionHash());

        assertFalse(credencial.getRequiereCambioClave());

        verify(repositorio).save(credencial);
    }
}
