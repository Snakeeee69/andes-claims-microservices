package com.andes.bpo.ms_siniestros.controller;

import com.andes.bpo.ms_siniestros.model.NotificacionSiniestro;
import com.andes.bpo.ms_siniestros.service.SiniestroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Tag(name = "6. Notificaciones", description = "Consulta de alertas automáticas generadas por eventos del siniestro y filtrado por palabra clave")
public class NotificacionController {
    private final SiniestroService siniestroService;

    public NotificacionController(SiniestroService siniestroService) {
        this.siniestroService = siniestroService;
    }

    @Operation(summary = "Buscar notificaciones por siniestro", description = "Lista las alertas de un siniestro. Permite filtrar por palabra clave dentro del mensaje (ej. palabra=pdf).")
    @GetMapping({"/api/notificaciones/siniestro/{siniestroId}", "/api/siniestros/{siniestroId}/notificaciones"})
    public ResponseEntity<List<NotificacionSiniestro>> buscarPorSiniestro(
            @Parameter(description = "ID del siniestro", example = "1") @PathVariable Long siniestroId,
            @Parameter(description = "Palabra clave opcional para filtrar mensajes", example = "pdf") @RequestParam(required = false) String palabra) {
        return ResponseEntity.ok(siniestroService.listarNotificacionesPorSiniestro(siniestroId, palabra));
    }

    @Operation(summary = "Listar notificaciones por usuario", description = "Obtiene todas las notificaciones dirigidas al RUT de un asegurado, analista o liquidador.")
    @GetMapping("/api/notificaciones/usuario/{rut}")
    public ResponseEntity<List<NotificacionSiniestro>> listarPorUsuario(
            @Parameter(description = "RUT del usuario destinatario", example = "12345678-9") @PathVariable String rut) {
        return ResponseEntity.ok(siniestroService.listarNotificacionesPorUsuario(rut));
    }

    @Operation(summary = "Marcar notificación como leída", description = "Actualiza el estado de una notificación específica a leída.")
    @PutMapping("/api/notificaciones/{idNotificacion}/leer")
    public ResponseEntity<NotificacionSiniestro> marcarLeida(
            @Parameter(description = "ID de la notificación", example = "1") @PathVariable Long idNotificacion) {
        return ResponseEntity.ok(siniestroService.marcarNotificacionLeida(idNotificacion));
    }
}