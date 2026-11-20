package com.andes.bpo.ms_auth_usuarios.controller;

import com.andes.bpo.ms_auth_usuarios.dto.AuthDtos.*;
import com.andes.bpo.ms_auth_usuarios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación y Seguridad", description = "Endpoints para inicio de sesión con JWT, cambio de clave obligatoria y recuperación de contraseña")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Iniciar sesión (Login)",
            description = "Valida el RUT y la contraseña, consulta los datos del perfil en ms-usuarios y genera un token JWT válido por 24 horas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa; devuelve el token JWT y los datos del usuario"),
            @ApiResponse(responseCode = "401", description = "RUT o contraseña incorrectos"),
            @ApiResponse(responseCode = "503", description = "Servicio de perfiles (ms-usuarios) no disponible")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Cambiar contraseña provisoria",
            description = "Actualiza la contraseña cuando el usuario inicia sesión por primera vez con la clave provisoria 123456 (requiereCambioClave = true)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "No se permite reutilizar la clave provisoria 123456"),
            @ApiResponse(responseCode = "404", description = "Credencial no encontrada para el usuario")
    })
    @PostMapping("/cambiar-clave-obligatoria")
    public ResponseEntity<MensajeResponse> cambiarClaveObligatoria(@RequestBody CambioClaveObligatorioRequest req) {
        authService.cambiarClaveObligatoria(req);
        return ResponseEntity.ok(new MensajeResponse("Contraseña actualizada correctamente"));
    }

    @Operation(
            summary = "Solicitar código de recuperación de contraseña",
            description = "Verifica que el RUT y el correo coincidan con los registrados en ms-usuarios y genera un código temporal de 8 caracteres."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Código de recuperación generado"),
            @ApiResponse(responseCode = "400", description = "El RUT y el correo no coinciden")
    })
    @PostMapping("/recuperar/solicitar")
    public ResponseEntity<MensajeResponse> solicitarCodigo(@RequestBody RecuperarRequest request) {
        String codigo = authService.solicitarCodigoRecuperacion(request);
        return ResponseEntity.ok(new MensajeResponse("Código generado: " + codigo));
    }

    @Operation(
            summary = "Confirmar nueva contraseña con código",
            description = "Valida el código temporal generado previamente y establece la nueva contraseña del usuario."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contraseña restablecida correctamente"),
            @ApiResponse(responseCode = "400", description = "Código de recuperación inválido o expirado")
    })
    @PostMapping("/recuperar/confirmar")
    public ResponseEntity<MensajeResponse> confirmarNuevaClave(@RequestBody ConfirmarClaveRequest request) {
        authService.confirmarNuevaClave(request);
        return ResponseEntity.ok(new MensajeResponse("Contraseña restablecida correctamente"));
    }

    @Operation(
            summary = "[Uso Interno] Crear credencial",
            description = "Endpoint interno invocado automáticamente por ms-usuarios al registrar un nuevo perfil. No debe llamarse directamente desde la app móvil."
    )
    @PostMapping("/interno/credenciales")
    public ResponseEntity<MensajeResponse> registrarCredencial(@RequestBody RegistroCredencialRequest req) {
        authService.registrarCredencialInterna(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(new MensajeResponse("Credencial creada"));
    }
}