package com.andes.bpo.ms_usuarios.service;

import com.andes.bpo.ms_usuarios.dto.ActualizarUsuarioRequest;
import com.andes.bpo.ms_usuarios.dto.RegistroCredencialRemoto;
import com.andes.bpo.ms_usuarios.dto.RegistroUsuarioRequest;
import com.andes.bpo.ms_usuarios.exception.RecursoNoEncontradoException;
import com.andes.bpo.ms_usuarios.exception.ReglaNegocioException;
import com.andes.bpo.ms_usuarios.model.Usuario;
import com.andes.bpo.ms_usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final JavaMailSender mailSender;

    @Value("${servicios.auth-uri}")
    private String authUri;

    private final RestClient restClient = RestClient.create();

    public String normalizarRut(String rut) {
        return rut == null ? "" : rut.replaceAll("[^0-9kK]", "").toUpperCase(Locale.ROOT);
    }

    public Usuario registrarUsuario(RegistroUsuarioRequest req) {
        String rutLimpio = normalizarRut(req.rut());
        String correoLimpio = req.correo().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByRut(rutLimpio)) {
            throw new ReglaNegocioException("Este RUT ya está registrado");
        }
        if (usuarioRepository.existsByCorreoIgnoreCase(correoLimpio)) {
            throw new ReglaNegocioException("Este correo ya está registrado");
        }
        if (req.fechaNacimiento() != null && req.fechaNacimiento().isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de nacimiento no puede ser futura");
        }

        Usuario nuevo = Usuario.builder()
                .rut(rutLimpio)
                .nombre(req.nombre().trim())
                .apellido(req.apellido().trim())
                .fechaNacimiento(req.fechaNacimiento())
                .correo(correoLimpio)
                .telefono(req.telefono() != null ? req.telefono().trim() : null)
                .direccion(req.direccion() != null ? req.direccion().trim() : null)
                .aseguradora(req.aseguradora() != null ? req.aseguradora().trim() : null)
                .rol(req.rol() != null && !req.rol().isBlank() ? req.rol() : "ASEGURADO_NATURAL")
                .equipo(req.equipo())
                .tipoLiquidador(req.tipoLiquidador())
                .idArea(req.idArea())
                .build();

        Usuario guardado = usuarioRepository.save(nuevo);

        try {
            String clave = (req.passwordHash() != null && !req.passwordHash().isBlank()) ? req.passwordHash() : "123456";
            restClient.post()
                    .uri(authUri + "/api/auth/interno/credenciales")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RegistroCredencialRemoto(guardado.getIdUsuario(), rutLimpio, clave, "123456".equals(clave)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            usuarioRepository.delete(guardado);
            throw new ReglaNegocioException("No se pudo sincronizar la credencial en el servicio de autenticación");
        }

        enviarCorreoBienvenida(guardado.getCorreo(), guardado.getNombre(), guardado.getRut());

        return guardado;
    }

    private void enviarCorreoBienvenida(String destinatario, String nombre, String rut) {
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage mensaje = new SimpleMailMessage();
                mensaje.setTo(destinatario);
                mensaje.setSubject("Bienvenido a Andes BPO - Registro exitoso");
                mensaje.setText(
                        "Hola " + nombre + ",\n\n" +
                        "Tu cuenta en Andes BPO ha sido registrada correctamente.\n" +
                        "RUT asociado: " + rut + "\n\n" +
                        "Ya puedes iniciar sesión en la aplicación para ingresar y hacer seguimiento a tus siniestros.\n\n" +
                        "Atentamente,\nEquipo Andes BPO"
                );
                mailSender.send(mensaje);
            } catch (Exception e) {
                System.err.println("No se pudo enviar correo de bienvenida: " + e.getMessage());
            }
        });
    }

    public Usuario obtenerPorRut(String rut) {
        return usuarioRepository.findByRut(normalizarRut(rut))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado para el RUT indicado"));
    }

    public Usuario obtenerPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    public Usuario actualizarUsuario(Integer id, ActualizarUsuarioRequest datos) {
        Usuario usuario = obtenerPorId(id);
        String correoLimpio = datos.correo().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByCorreoIgnoreCaseAndIdUsuarioNot(correoLimpio, id)) {
            throw new ReglaNegocioException("El correo ya pertenece a otro usuario");
        }

        usuario.setNombre(datos.nombre().trim());
        usuario.setApellido(datos.apellido().trim());
        if (datos.fechaNacimiento() != null) usuario.setFechaNacimiento(datos.fechaNacimiento());
        usuario.setCorreo(correoLimpio);
        if (datos.telefono() != null) usuario.setTelefono(datos.telefono().trim());
        if (datos.direccion() != null) usuario.setDireccion(datos.direccion().trim());
        if (datos.aseguradora() != null) usuario.setAseguradora(datos.aseguradora().trim());
        if (datos.rol() != null && !datos.rol().isBlank()) usuario.setRol(datos.rol());
        if (datos.equipo() != null) usuario.setEquipo(datos.equipo());
        if (datos.tipoLiquidador() != null) usuario.setTipoLiquidador(datos.tipoLiquidador());
        if (datos.idArea() != null) usuario.setIdArea(datos.idArea());

        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarLiquidadoresPorEquipo(String equipo) {
        return usuarioRepository.findByEquipoAndRolStartingWith(equipo.toUpperCase(Locale.ROOT), "LIQUIDADOR");
    }
}