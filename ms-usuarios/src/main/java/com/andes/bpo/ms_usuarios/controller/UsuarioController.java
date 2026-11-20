package com.andes.bpo.ms_usuarios.controller;

import com.andes.bpo.ms_usuarios.dto.ActualizarUsuarioRequest;
import com.andes.bpo.ms_usuarios.dto.RegistroUsuarioRequest;
import com.andes.bpo.ms_usuarios.model.Usuario;
import com.andes.bpo.ms_usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Tag(name = "Gestión de Usuarios", description = "Registro, consulta y actualización de perfiles de asegurados, analistas y liquidadores")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Operation(
            summary = "Registrar un nuevo usuario",
            description = "Crea el perfil del usuario (RUT, nombre, apellido, fecha de nacimiento d/M/yyyy, aseguradora) y sincroniza su contraseña en ms-auth-usuarios."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, RUT duplicado o correo ya registrado")
    })
    @PostMapping("/registro")
    public ResponseEntity<Usuario> registrarUsuario(@Valid @RequestBody RegistroUsuarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrarUsuario(req));
    }

    @Operation(
            summary = "Buscar usuario por RUT",
            description = "Obtiene el perfil completo de un usuario a partir de su RUT (con o sin puntos y guión)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un usuario con el RUT indicado")
    })
    @GetMapping("/rut/{rut}")
    public ResponseEntity<Usuario> obtenerPorRut(
            @Parameter(description = "RUT del usuario", example = "158423715")
            @PathVariable String rut) {
        return ResponseEntity.ok(usuarioService.obtenerPorRut(rut));
    }

    @Operation(
            summary = "Buscar usuario por ID",
            description = "Obtiene el perfil completo mediante su identificador numérico interno."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> obtenerPorId(
            @Parameter(description = "ID interno del usuario", example = "1")
            @PathVariable Integer id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
    }

    @Operation(
            summary = "Actualizar perfil de usuario",
            description = "Modifica datos personales como nombre, apellido, fecha de nacimiento, correo, teléfono, dirección o aseguradora."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Correo duplicado o datos con formato inválido"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> actualizarUsuario(
            @Parameter(description = "ID interno del usuario a editar", example = "1")
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarUsuarioRequest datos) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, datos));
    }

    @Operation(
            summary = "Listar todos los usuarios",
            description = "Devuelve el listado completo de perfiles registrados en la plataforma."
    )
    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @Operation(
            summary = "Listar liquidadores por equipo",
            description = "Filtra los liquidadores asignados a un equipo o ramo específico (ej: VEHICULOS)."
    )
    @GetMapping("/liquidadores/{equipo}")
    public ResponseEntity<List<Usuario>> listarLiquidadoresPorEquipo(
            @Parameter(description = "Nombre del equipo o ramo", example = "VEHICULOS")
            @PathVariable String equipo) {
        return ResponseEntity.ok(usuarioService.listarLiquidadoresPorEquipo(equipo));
    }
}