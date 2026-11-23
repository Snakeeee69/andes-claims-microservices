package com.andes.bpo.ms_siniestros.controller;

import com.andes.bpo.ms_siniestros.model.AsignacionAnalista;
import com.andes.bpo.ms_siniestros.service.SiniestroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "3. Asignación de Analistas", description = "Asignación y trazabilidad de analistas evaluadores internos en los siniestros")
public class AsignacionController {
    private final SiniestroService siniestroService;

    public AsignacionController(SiniestroService siniestroService) {
        this.siniestroService = siniestroService;
    }

    @Operation(summary = "Asignar analista (por body JSON)", description = "Asigna un analista evaluador enviando siniestroId, rutAnalista, asignadoPor y observacion en el cuerpo de la petición.")
    @PostMapping("/api/asignaciones/analista")
    public ResponseEntity<AsignacionAnalista> asignar(@RequestBody Map<String, Object> body) {
        Long siniestroId = Long.valueOf(body.get("siniestroId").toString());
        String rutAnalista = body.get("rutAnalista").toString();
        String asignadoPor = body.get("asignadoPor") != null ? body.get("asignadoPor").toString() : "SUPERVISOR";
        String obs = body.get("observacion") != null ? body.get("observacion").toString() : "";
        return ResponseEntity.ok(siniestroService.asignarAnalista(siniestroId, rutAnalista, asignadoPor, obs));
    }

    @Operation(summary = "Asignar analista (por ID en ruta)", description = "Asigna o reasigna un analista evaluador indicando el ID del siniestro directamente en la URL.")
    @PutMapping("/api/siniestros/{siniestroId}/asignar-analista")
    public ResponseEntity<AsignacionAnalista> asignarPorPath(
            @Parameter(description = "ID del siniestro", example = "1") @PathVariable Long siniestroId,
            @RequestBody Map<String, Object> body) {
        String rutAnalista = body.get("rutAnalista").toString();
        String asignadoPor = body.get("asignadoPor") != null ? body.get("asignadoPor").toString() : "SUPERVISOR";
        String obs = body.get("observacion") != null ? body.get("observacion").toString() : "";
        return ResponseEntity.ok(siniestroService.asignarAnalista(siniestroId, rutAnalista, asignadoPor, obs));
    }

    @Operation(summary = "Listar asignaciones de un siniestro", description = "Obtiene el historial de analistas que han sido asignados a un siniestro.")
    @GetMapping("/api/asignaciones/siniestro/{siniestroId}")
    public ResponseEntity<List<AsignacionAnalista>> listarPorSiniestro(
            @Parameter(description = "ID del siniestro", example = "1") @PathVariable Long siniestroId) {
        return ResponseEntity.ok(siniestroService.listarAsignacionesPorSiniestro(siniestroId));
    }
}