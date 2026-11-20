package com.andes.bpo.ms_auth_usuarios.service;

import com.andes.bpo.ms_auth_usuarios.dto.AuthDtos.*;
import com.andes.bpo.ms_auth_usuarios.exception.AuthException;
import com.andes.bpo.ms_auth_usuarios.model.Credencial;
import com.andes.bpo.ms_auth_usuarios.repository.CredencialRepository;
import com.andes.bpo.ms_auth_usuarios.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CredencialRepository credencialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${servicios.usuarios-uri}")
    private String usuariosUri;

    private final RestClient restClient = RestClient.create();

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

        return new AuthResponse(token, perfilUsuario);
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

        try {
            Map<String, Object> usuario = restClient.get()
                    .uri(usuariosUri + "/api/usuarios/rut/" + rutLimpio)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            String correoRegistrado = usuario != null ? String.valueOf(usuario.get("correo")) : "";
            if (!correoRegistrado.equalsIgnoreCase(request.correo().trim())) {
                throw new AuthException("El RUT y el correo no coinciden", HttpStatus.BAD_REQUEST);
            }
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception e) {
            throw new AuthException("Error consultando ms-usuarios", HttpStatus.SERVICE_UNAVAILABLE);
        }

        String codigo = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        cred.setCodigoRecuperacionHash(passwordEncoder.encode(codigo));
        credencialRepository.save(cred);
        return codigo;
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