
package com.andes.bpo.ms_auth_usuarios.service;

import com.andes.bpo.ms_auth_usuarios.dto.AuthDtos.*;
import com.andes.bpo.ms_auth_usuarios.exception.AuthException;
import com.andes.bpo.ms_auth_usuarios.model.Credencial;
import com.andes.bpo.ms_auth_usuarios.repository.CredencialRepository;
import com.andes.bpo.ms_auth_usuarios.security.JwtUtil;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AuthServiceCoberturaTest {

    private CredencialRepository repositorio;
    private PasswordEncoder encoder;
    private JwtUtil jwt;
    private JavaMailSender correo;
    private AuthService servicio;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        repositorio = mock(CredencialRepository.class);
        encoder = mock(PasswordEncoder.class);
        jwt = mock(JwtUtil.class);
        correo = mock(JavaMailSender.class);

        servicio = new AuthService(
                repositorio, encoder, jwt, correo
        );

        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();

        ReflectionTestUtils.setField(
                servicio, "restClient", builder.build()
        );

        ReflectionTestUtils.setField(
                servicio, "usuariosUri", "http://usuarios-prueba"
        );

        ReflectionTestUtils.setField(
                servicio, "correoRemitente", "soporte@prueba.cl"
        );
    }

    private Credencial credencial() {
        return Credencial.builder()
                .idUsuario(10)
                .rut("12345678K")
                .passwordHash("hash-original")
                .requiereCambioClave(false)
                .build();
    }

    private void simularPerfil(String json) {
        servidor.expect(requestTo(
                "http://usuarios-prueba/api/usuarios/rut/12345678K"
        )).andRespond(withSuccess(
                json, MediaType.APPLICATION_JSON
        ));
    }

    @Test
    void loginCorrectoGeneraTokenYDevuelvePerfil() {
        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.of(credencial()));

        when(encoder.matches("Clave1234", "hash-original"))
                .thenReturn(true);

        when(jwt.generarToken("12345678K", "LIQUIDADOR", 10))
                .thenReturn("token-de-prueba");

        simularPerfil("""
                {"rol":"LIQUIDADOR","idUsuario":10,"nombre":"Ana"}
                """);

        AuthResponse resultado = servicio.login(
                new LoginRequest("12.345.678-k", "Clave1234")
        );

        assertEquals("token-de-prueba", resultado.token());
        assertEquals("LIQUIDADOR", resultado.usuario().get("rol"));
        assertEquals("Ana", resultado.usuario().get("nombre"));
        assertEquals(false,
                resultado.usuario().get("requiereCambioClave"));

        servidor.verify();
    }

    @Test
    void loginConClaveProvisoriaObligaCambio() {
        Credencial c = credencial();

        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.of(c));

        when(encoder.matches("123456", "hash-original"))
                .thenReturn(true);

        simularPerfil("""
                {"rol":"ASEGURADO_NATURAL","idUsuario":10}
                """);

        servicio.login(
                new LoginRequest("12345678K", "123456")
        );

        assertTrue(c.getRequiereCambioClave());
        verify(repositorio).save(c);
        servidor.verify();
    }

    @Test
    void loginSinDatosDePerfilUsaValoresPredeterminados() {
        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.of(credencial()));

        when(encoder.matches("Clave1234", "hash-original"))
                .thenReturn(true);

        servidor.expect(requestTo(
                "http://usuarios-prueba/api/usuarios/rut/12345678K"
        )).andRespond(withNoContent());

        servicio.login(
                new LoginRequest("12345678K", "Clave1234")
        );

        verify(jwt).generarToken(
                "12345678K", "ASEGURADO_NATURAL", 10
        );

        servidor.verify();
    }

    @Test
    void loginCuandoUsuariosNoResponde() {
        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.of(credencial()));

        when(encoder.matches("Clave1234", "hash-original"))
                .thenReturn(true);

        servidor.expect(requestTo(
                "http://usuarios-prueba/api/usuarios/rut/12345678K"
        )).andRespond(withStatus(
                HttpStatus.SERVICE_UNAVAILABLE
        ));

        AuthException error = assertThrows(
                AuthException.class,
                () -> servicio.login(
                        new LoginRequest("12345678K", "Clave1234")
                )
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                error.getStatus());

        servidor.verify();
    }

    @Test
    void recuperacionCorrectaGeneraCodigoYEnviaCorreo() {
        Credencial c = credencial();

        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.of(c));

        when(encoder.encode(anyString()))
                .thenReturn("hash-del-codigo");

        MimeMessage mensaje = new MimeMessage(
                Session.getInstance(new Properties())
        );

        when(correo.createMimeMessage())
                .thenReturn(mensaje);

        simularPerfil("""
                {"nombre":"Ana","correo":"ana@correo.cl"}
                """);

        String resultado = servicio.solicitarCodigoRecuperacion(
                new RecuperarRequest(
                        "12.345.678-k", "ana@correo.cl"
                )
        );

        assertTrue(resultado.contains("ana@correo.cl"));
        assertEquals("hash-del-codigo",
                c.getCodigoRecuperacionHash());

        verify(encoder).encode(argThat(
        codigo -> java.util.regex.Pattern.matches("\\d{6}", codigo)
        ));

        verify(repositorio).save(c);
        verify(correo).send(any(MimeMessage.class));

        servidor.verify();
    }

    @Test
    void recuperacionRechazaCorreoIncorrecto() {
        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.of(credencial()));

        simularPerfil("""
                {"nombre":"Ana","correo":"ana@correo.cl"}
                """);

        assertThrows(AuthException.class,
                () -> servicio.solicitarCodigoRecuperacion(
                        new RecuperarRequest(
                                "12345678K", "otro@correo.cl"
                        )
                ));

        verify(repositorio, never()).save(any());
        verify(correo, never())
                .send(any(MimeMessage.class));

        servidor.verify();
    }

    @Test
    void recuperacionManejaFalloDelMicroservicioUsuarios() {
        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.of(credencial()));

        servidor.expect(requestTo(
                "http://usuarios-prueba/api/usuarios/rut/12345678K"
        )).andRespond(withStatus(
                HttpStatus.SERVICE_UNAVAILABLE
        ));

        AuthException error = assertThrows(
                AuthException.class,
                () -> servicio.solicitarCodigoRecuperacion(
                        new RecuperarRequest(
                                "12345678K", "ana@correo.cl"
                        )
                )
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                error.getStatus());

        servidor.verify();
    }

    @Test
    void recuperacionManejaErrorDeEnvioDeCorreo() {
        when(repositorio.findByRut("12345678K"))
                .thenReturn(Optional.of(credencial()));

        when(encoder.encode(anyString()))
                .thenReturn("hash-codigo");

        when(correo.createMimeMessage())
                .thenThrow(new RuntimeException("SMTP no disponible"));

        simularPerfil("""
                {"nombre":"Ana","correo":"ana@correo.cl"}
                """);

        AuthException error = assertThrows(
                AuthException.class,
                () -> servicio.solicitarCodigoRecuperacion(
                        new RecuperarRequest(
                                "12345678K", "ana@correo.cl"
                        )
                )
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                error.getStatus());

        assertTrue(error.getMessage().contains(
                "No se pudo enviar el correo"
        ));

        servidor.verify();
    }
}
