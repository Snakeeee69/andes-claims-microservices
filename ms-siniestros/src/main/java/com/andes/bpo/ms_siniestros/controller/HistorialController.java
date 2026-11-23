package com.andes.bpo.ms_siniestros.controller;

import com.andes.bpo.ms_siniestros.model.HistorialSiniestro;
import com.andes.bpo.ms_siniestros.service.SiniestroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Tag(name = "5. Historial y Auditoría", description = "Bitácora cronológica de eventos, cambios de estado y acciones realizadas sobre un siniestro")
public class HistorialController {
    private final SiniestroService siniestroService;

    public HistorialController(SiniestroService siniestroService) {
        this.siniestroService = siniestroService;
    }

    @Operation(summary = "Consultar historial del siniestro", description = "Obtiene la línea de tiempo completa de auditoría (creación, asignaciones, subida de documentos, etapas y cierre).")
    @GetMapping({"/api/historial/siniestro/{siniestroId}", "/api/siniestros/{siniestroId}/historial"})
    public ResponseEntity<List<HistorialSiniestro>> listar(
            @Parameter(description = "ID del siniestro", example = "1") @PathVariable Long siniestroId) {
        return ResponseEntity.ok(siniestroService.listarHistorial(siniestroId));
    }
}