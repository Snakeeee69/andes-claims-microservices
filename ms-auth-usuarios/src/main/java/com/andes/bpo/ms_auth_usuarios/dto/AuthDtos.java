package com.andes.bpo.ms_auth_usuarios.dto;

import java.util.Map;

public class AuthDtos {
    public record LoginRequest(String rut, String clave) {}
    public record AuthResponse(String token, Map<String, Object> usuario) {}
    public record RecuperarRequest(String rut, String correo) {}
    public record ConfirmarClaveRequest(String rut, String codigo, String nuevaClave) {}
    public record CambioClaveObligatorioRequest(Integer idUsuario, String nuevaClave) {}
    public record RegistroCredencialRequest(Integer idUsuario, String rut, String clavePlana, Boolean requiereCambioClave) {}
    public record MensajeResponse(String mensaje) {}
}