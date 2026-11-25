package com.andes.bpo.ms_auth_usuarios.service;

import com.andes.bpo.ms_auth_usuarios.dto.AuthDtos.*;
import com.andes.bpo.ms_auth_usuarios.exception.AuthException;
import com.andes.bpo.ms_auth_usuarios.model.Credencial;
import com.andes.bpo.ms_auth_usuarios.repository.CredencialRepository;
import com.andes.bpo.ms_auth_usuarios.security.JwtUtil;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CredencialRepository credencialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;

    @Value("${servicios.usuarios-uri}")
    private String usuariosUri;

    @Value("${spring.mail.username}")
    private String correoRemitente;

    private final RestClient restClient = RestClient.create();
    private final SecureRandom secureRandom = new SecureRandom();

    public String normalizarRut(String rut) {
        return rut == null ? "" : rut.replaceAll("[^0-9kK]", "").toUpperCase(Locale.ROOT);
    }

    public AuthResponse login(LoginRequest request) {
        String rutLimpio = normalizarRut(request.rut());
        Credencial cred = credencialRepository.findByRut(rutLimpio)
                .orElseThrow(() -> new AuthException("RUT o contraseña incorrectos", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.clave(), cred.getPasswordHash())) {
            throw new AuthException("RUT o contraseña incorrectos", HttpStatus.UNAUTHORIZED);
        }

        if ("123456".equals(request.clave()) && !Boolean.TRUE.equals(cred.getRequiereCambioClave())) {
            cred.setRequiereCambioClave(true);
            credencialRepository.save(cred);
        }

        Map<String, Object> perfilUsuario;
        try {
            Map<String, Object> remoto = restClient.get()
                    .uri(usuariosUri + "/api/usuarios/rut/" + rutLimpio)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            perfilUsuario = remoto != null ? new HashMap<>(remoto) : new HashMap<>();
        } catch (Exception e) {
            throw new AuthException("No se pudo obtener el perfil desde ms-usuarios", HttpStatus.SERVICE_UNAVAILABLE);
        }

        perfilUsuario.put("requiereCambioClave", cred.getRequiereCambioClave());
        String rol = String.valueOf(perfilUsuario.getOrDefault("rol", "ASEGURADO_NATURAL"));
        Integer idUsuario = perfilUsuario.get("idUsuario") instanceof Number n ? n.intValue() : cred.getIdUsuario();
        String token = jwtUtil.generarToken(rutLimpio, rol, idUsuario);

        Object correoObj = perfilUsuario.get("correo");
        if (correoObj != null && !String.valueOf(correoObj).isBlank()) {
            String correoDestino = String.valueOf(correoObj).trim();
            String nombreUsuario = perfilUsuario.get("nombre") != null
                    ? String.valueOf(perfilUsuario.get("nombre")).trim()
                    : "Asegurado";
            enviarCorreoInicioSesion(correoDestino, nombreUsuario);
        }

        return new AuthResponse(token, perfilUsuario);
    }

    private void enviarCorreoInicioSesion(String destinatario, String nombre) {
        CompletableFuture.runAsync(() -> {
            try {
                String fechaHora = ZonedDateTime.now(ZoneId.of("America/Santiago"))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

                MimeMessage mensaje = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
                helper.setFrom(correoRemitente, "Andes BPO - Seguridad");
                helper.setTo(destinatario);
                helper.setSubject("Nuevo inicio de sesión detectado - Andes BPO");

                String html = """
                        <div style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px;">
                          <h2 style="color: #0f172a; margin-bottom: 8px;">Nuevo inicio de sesión</h2>
                          <p style="color: #334155; font-size: 15px;">Hola <strong>%s</strong>,</p>
                          <p style="color: #334155; font-size: 15px;">Se ha registrado un inicio de sesión en tu cuenta de <strong>Andes BPO</strong>.</p>
                          <div style="background-color: #f1f5f9; border-radius: 8px; padding: 14px; margin: 18px 0;">
                            <p style="margin: 0; color: #0f172a; font-size: 14px;"><strong>Fecha y hora:</strong> %s</p>
                          </div>
                          <p style="color: #64748b; font-size: 13px;">Si fuiste tú, puedes ignorar este mensaje. Si no reconoces este acceso, te recomendamos cambiar tu contraseña desde la aplicación.</p>
                        </div>
                        """.formatted(nombre, fechaHora);

                helper.setText(html, true);
                mailSender.send(mensaje);
            } catch (Exception e) {
                System.err.println("No se pudo enviar correo de inicio de sesión: " + e.getMessage());
            }
        });
    }

    public void registrarCredencialInterna(RegistroCredencialRequest req) {
        String rutLimpio = normalizarRut(req.rut());
        if (credencialRepository.existsByRut(rutLimpio)) {
            throw new AuthException("Credencial ya existente", HttpStatus.BAD_REQUEST);
        }
        String clave = (req.clavePlana() != null && !req.clavePlana().isBlank()) ? req.clavePlana() : "123456";
        Credencial nueva = Credencial.builder()
                .idUsuario(req.idUsuario())
                .rut(rutLimpio)
                .passwordHash(passwordEncoder.encode(clave))
                .requiereCambioClave(Boolean.TRUE.equals(req.requiereCambioClave()) || "123456".equals(clave))
                .build();
        credencialRepository.save(nueva);
    }

    public void cambiarClaveObligatoria(CambioClaveObligatorioRequest req) {
        if ("123456".equals(req.nuevaClave())) {
            throw new AuthException("No puedes reutilizar la clave provisoria 123456", HttpStatus.BAD_REQUEST);
        }
        Credencial cred = credencialRepository.findByIdUsuario(req.idUsuario())
                .orElseThrow(() -> new AuthException("Credencial no encontrada", HttpStatus.NOT_FOUND));
        cred.setPasswordHash(passwordEncoder.encode(req.nuevaClave()));
        cred.setRequiereCambioClave(false);
        credencialRepository.save(cred);
    }

    public String solicitarCodigoRecuperacion(RecuperarRequest request) {
        String rutLimpio = normalizarRut(request.rut());
        Credencial cred = credencialRepository.findByRut(rutLimpio)
                .orElseThrow(() -> new AuthException("El RUT y el correo no coinciden", HttpStatus.BAD_REQUEST));

        String nombreUsuario = "Asegurado";
        String correoRegistrado;
        try {
            Map<String, Object> usuario = restClient.get()
                    .uri(usuariosUri + "/api/usuarios/rut/" + rutLimpio)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            correoRegistrado = usuario != null ? String.valueOf(usuario.get("correo")).trim() : "";
            if (usuario != null && usuario.get("nombre") != null) {
                nombreUsuario = String.valueOf(usuario.get("nombre"));
            }
            if (!correoRegistrado.equalsIgnoreCase(request.correo().trim())) {
                throw new AuthException("El RUT y el correo no coinciden", HttpStatus.BAD_REQUEST);
            }
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception e) {
            throw new AuthException("Error consultando ms-usuarios", HttpStatus.SERVICE_UNAVAILABLE);
        }

        String codigo = String.format("%06d", secureRandom.nextInt(1_000_000));
        cred.setCodigoRecuperacionHash(passwordEncoder.encode(codigo));
        credencialRepository.save(cred);

        enviarCorreoRecuperacion(correoRegistrado, nombreUsuario, codigo);
        return "Código de verificación enviado a " + correoRegistrado;
    }

    private void enviarCorreoRecuperacion(String destinatario, String nombre, String codigo) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(correoRemitente, "Andes BPO - Soporte");
            helper.setTo(destinatario);
            helper.setSubject("Código de recuperación de contraseña - Andes BPO");

            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; border: 1px solid #e2e8f0; border-radius: 12px;">
                      <h2 style="color: #0f172a; margin-bottom: 8px;">Recuperación de contraseña</h2>
                      <p style="color: #334155; font-size: 15px;">Hola <strong>%s</strong>,</p>
                      <p style="color: #334155; font-size: 15px;">Ingresa el siguiente código de verificación en la aplicación <strong>Andes BPO</strong> para restablecer tu contraseña:</p>
                      <div style="background-color: #f1f5f9; border-radius: 8px; padding: 16px; text-align: center; margin: 20px 0;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #1d4ed8;">%s</span>
                      </div>
                      <p style="color: #64748b; font-size: 13px;">Si no solicitaste este cambio, puedes ignorar este correo de forma segura.</p>
                    </div>
                    """.formatted(nombre, codigo);

            helper.setText(html, true);
            mailSender.send(mensaje);
        } catch (Exception e) {
            throw new AuthException("No se pudo enviar el correo de verificación: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void confirmarNuevaClave(ConfirmarClaveRequest request) {
        String rutLimpio = normalizarRut(request.rut());
        Credencial cred = credencialRepository.findByRut(rutLimpio)
                .orElseThrow(() -> new AuthException("Código de recuperación inválido", HttpStatus.BAD_REQUEST));

        if (cred.getCodigoRecuperacionHash() == null ||
                !passwordEncoder.matches(request.codigo().trim().toUpperCase(Locale.ROOT), cred.getCodigoRecuperacionHash())) {
            throw new AuthException("Código de recuperación inválido", HttpStatus.BAD_REQUEST);
        }

        cred.setPasswordHash(passwordEncoder.encode(request.nuevaClave()));
        cred.setCodigoRecuperacionHash(null);
        cred.setRequiereCambioClave(false);
        credencialRepository.save(cred);
    }
}