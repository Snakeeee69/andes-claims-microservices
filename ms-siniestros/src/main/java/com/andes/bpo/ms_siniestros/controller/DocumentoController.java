package com.andes.bpo.ms_siniestros.controller;

import com.andes.bpo.ms_siniestros.model.DocumentoSiniestro;
import com.andes.bpo.ms_siniestros.service.SiniestroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Tag(name = "4. Documentos y Evidencias", description = "Carga y consulta de respaldos digitales (PDF, fotos, presupuestos) asociados a un siniestro")
public class DocumentoController {
    private final SiniestroService siniestroService;

    public DocumentoController(SiniestroService siniestroService) {
        this.siniestroService = siniestroService;
    }

    @Operation(summary = "Subir documento al siniestro", description = "Adjunta un documento o evidencia al siniestro y dispara una notificación automática indicando el tipo de archivo (ej. PDF).")
    @PostMapping({"/api/documentos/siniestro/{siniestroId}", "/api/siniestros/{siniestroId}/documentos"})
    public ResponseEntity<DocumentoSiniestro> subir(
            @Parameter(description = "ID del siniestro", example = "1") @PathVariable Long siniestroId,
            @RequestBody DocumentoSiniestro doc) {
        return ResponseEntity.ok(siniestroService.agregarDocumento(siniestroId, doc));
    }

    @Operation(summary = "Listar documentos del siniestro", description = "Devuelve todos los documentos y respaldos digitales cargados para un siniestro.")
    @GetMapping({"/api/documentos/siniestro/{siniestroId}", "/api/siniestros/{siniestroId}/documentos"})
    public ResponseEntity<List<DocumentoSiniestro>> listar(
            @Parameter(description = "ID del siniestro", example = "1") @PathVariable Long siniestroId) {
        return ResponseEntity.ok(siniestroService.listarDocumentos(siniestroId));
    }
}