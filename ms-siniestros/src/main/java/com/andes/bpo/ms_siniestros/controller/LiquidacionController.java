package com.andes.bpo.ms_siniestros.controller;

import com.andes.bpo.ms_siniestros.dto.LiquidadorDTO;
import com.andes.bpo.ms_siniestros.model.LiquidacionGestion;
import com.andes.bpo.ms_siniestros.service.LiquidacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "7. Liquidación (D.S. N° 1.055)", description = "Gestión integral de liquidación: consulta de liquidadores en ms-usuarios, asignación, aceptación, etapas y cierre")
public class LiquidacionController {
    private final LiquidacionService liquidacionService;

    public LiquidacionController(LiquidacionService liquidacionService) {
        this.liquidacionService = liquidacionService;
    }

    @Operation(summary = "Listar liquidadores disponibles", description = "Consulta en vivo a ms-usuarios los liquidadores activos, con filtro opcional por tipo (DIRECTO/EXTERNO) o área.")
    @GetMapping({"/api/liquidaciones/liquidadores", "/api/liquidacion/liquidadores"})
    public ResponseEntity<List<LiquidadorDTO>> listarLiquidadores(
            @Parameter(description = "Tipo de liquidador (DIRECTO o EXTERNO)") @RequestParam(required = false) String tipo,
            @Parameter(description = "ID del área de especialidad") @RequestParam(required = false) Integer idArea,
            @Parameter(description = "Alias alternativo para ID de área") @RequestParam(required = false) Integer areaId) {
        return ResponseEntity.ok(liquidacionService.listarLiquidadores(tipo, idArea != null ? idArea : areaId));
    }

    @Operation(summary = "Asignar liquidador al siniestro", description = "Deriva el siniestro a un liquidador oficial y establece el plazo legal de respuesta de 2 días hábiles.")
    @PostMapping({"/api/liquidaciones/asignar", "/api/liquidacion/asignar"})
    public ResponseEntity<LiquidacionGestion> asignar(@RequestBody Map<String, Object> body) {
        Long siniestroId = Long.valueOf(body.get("siniestroId").toString());
        String rutLiquidador = body.get("rutLiquidador").toString();
        return ResponseEntity.ok(liquidacionService.asignarCaso(siniestroId, rutLiquidador));
    }

    @Operation(summary = "Obtener gestión de liquidación por siniestro", description = "Consulta el estado actual, fechas, etapa y monto de indemnización de la liquidación de un siniestro.")
    @GetMapping({"/api/liquidaciones/siniestro/{siniestroId}", "/api/liquidacion/siniestro/{siniestroId}"})
    public ResponseEntity<LiquidacionGestion> obtenerPorSiniestro(
            @Parameter(description = "ID del siniestro", example = "1") @PathVariable Long siniestroId) {
        return ResponseEntity.ok(liquidacionService.obtenerPorSiniestro(siniestroId));
    }

    @Operation(summary = "Listar casos de un liquidador", description = "Devuelve todas las liquidaciones asignadas a un liquidador según su RUT.")
    @GetMapping({"/api/liquidaciones/liquidador/{rut}", "/api/liquidacion/liquidador/{rut}"})
    public ResponseEntity<List<LiquidacionGestion>> casosPorLiquidador(
            @Parameter(description = "RUT del liquidador", example = "18111222-3") @PathVariable String rut) {
        return ResponseEntity.ok(liquidacionService.listarCasosPorLiquidador(rut));
    }

    @Operation(summary = "Aceptar caso de liquidación", description = "El liquidador acepta el caso asignado e inicia formalmente el proceso de inspección y evaluación.")
    @PutMapping("/api/siniestros/{siniestroId}/aceptar")
    public ResponseEntity<LiquidacionGestion> aceptar(
            @Parameter(description = "ID del siniestro", example = "1") @PathVariable Long siniestroId,
            @RequestBody(required = false) Map<String, Object> body) {
        String rut = (body != null && body.get("rutLiquidador") != null) ? body.get("rutLiquidador").toString() : null;
        return ResponseEntity.ok(liquidacionService.aceptarCaso(siniestroId, rut));
    }

    @Operation(summary = "Rechazar caso de liquidación", description = "El liquidador rechaza o se inhabilita del caso indicando el motivo de rechazo.")
    @PutMapping("/api/siniestros/{siniestroId}/rechazar")
    public ResponseEntity<LiquidacionGestion> rechazar(
            @Parameter(description = "ID del siniestro", example = "1") @PathVariable Long siniestroId,
            @RequestBody Map<String, Object> body) {
        String rut = body.get("rutLiquidador") != null ? body.get("rutLiquidador").toString() : null;
        String motivo = body.get("motivoRechazo") != null ? body.get("motivoRechazo").toString() : "";
        return ResponseEntity.ok(liquidacionService.rechazarCaso(siniestroId, rut, motivo));
    }

    @Operation(summary = "Actualizar etapa de liquidación", description = "Avanza la etapa operativa del caso (ej. INSPECCION, INFORME_TECNICO, REVISION_FINAL).")
    @PutMapping("/api/siniestros/{siniestroId}/etapa")
    public ResponseEntity<LiquidacionGestion> cambiarEtapa(
            @Parameter(description = "ID del siniestro", example = "1") @PathVariable Long siniestroId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(liquidacionService.actualizarEtapa(siniestroId, body.get("etapa").toString()));
    }

    @Operation(summary = "Cerrar liquidación e indemnización", description = "Finaliza el proceso de liquidación registrando el monto aprobado de indemnización y el informe técnico final.")
    @PutMapping("/api/siniestros/{siniestroId}/cerrar")
    public ResponseEntity<LiquidacionGestion> cerrar(
            @Parameter(description = "ID del siniestro", example = "1") @PathVariable Long siniestroId,
            @RequestBody Map<String, Object> body) {
        String rut = body.get("rutLiquidador") != null ? body.get("rutLiquidador").toString() : null;
        Double monto = body.get("montoIndemnizacion") != null ? Double.valueOf(body.get("montoIndemnizacion").toString()) : 0.0;
        String informe = body.get("informeLiquidacion") != null ? body.get("informeLiquidacion").toString() : "";
        return ResponseEntity.ok(liquidacionService.cerrarLiquidacion(siniestroId, rut, monto, informe));
    }
}