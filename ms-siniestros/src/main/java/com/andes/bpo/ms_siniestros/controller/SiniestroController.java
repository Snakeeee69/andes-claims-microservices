package com.andes.bpo.ms_siniestros.controller;

import com.andes.bpo.ms_siniestros.model.Siniestro;
import com.andes.bpo.ms_siniestros.service.SiniestroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/siniestros")
@Tag(name = "2. Siniestros (Denuncios)", description = "Ingreso y consulta general de siniestros por ID, asegurado, analista o liquidador")
public class SiniestroController {
    private final SiniestroService siniestroService;

    public SiniestroController(SiniestroService siniestroService) {
        this.siniestroService = siniestroService;
    }

    @Operation(summary = "Listar todos los siniestros", description = "Devuelve el listado completo de siniestros registrados en el sistema.")
    @GetMapping
    public ResponseEntity<List<Siniestro>> listarTodos() {
        return ResponseEntity.ok(siniestroService.listarTodos());
    }

    @Operation(summary = "Obtener siniestro por ID", description = "Busca el detalle completo de un siniestro específico mediante su identificador numérico.")
    @GetMapping("/{id}")
    public ResponseEntity<Siniestro> obtenerPorId(
            @Parameter(description = "ID numérico del siniestro", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(siniestroService.obtenerPorId(id));
    }

    @Operation(summary = "Buscar siniestros por asegurado", description = "Lista todos los siniestros asociados al RUT de un cliente asegurado.")
    @GetMapping("/asegurado/{rut}")
    public ResponseEntity<List<Siniestro>> porAsegurado(
            @Parameter(description = "RUT del asegurado", example = "12345678-9") @PathVariable String rut) {
        return ResponseEntity.ok(siniestroService.listarPorAsegurado(rut));
    }

    @Operation(summary = "Buscar siniestros por analista", description = "Lista los siniestros asignados a un analista evaluador según su RUT.")
    @GetMapping("/analista/{rut}")
    public ResponseEntity<List<Siniestro>> porAnalista(
            @Parameter(description = "RUT del analista", example = "15999888-7") @PathVariable String rut) {
        return ResponseEntity.ok(siniestroService.listarPorAnalista(rut));
    }

    @Operation(summary = "Buscar siniestros por liquidador", description = "Lista los siniestros asignados a un liquidador según su RUT.")
    @GetMapping("/liquidador/{rut}")
    public ResponseEntity<List<Siniestro>> porLiquidador(
            @Parameter(description = "RUT del liquidador", example = "18111222-3") @PathVariable String rut) {
        return ResponseEntity.ok(siniestroService.listarPorLiquidador(rut));
    }

    @Operation(summary = "Crear nuevo denuncio de siniestro", description = "Registra un nuevo siniestro, genera su código único (SIN-...) y deja trazabilidad inicial en historial y notificaciones.")
    @PostMapping
    public ResponseEntity<Siniestro> crear(@RequestBody Siniestro siniestro) {
        return ResponseEntity.ok(siniestroService.crearDenuncio(siniestro));
    }
}