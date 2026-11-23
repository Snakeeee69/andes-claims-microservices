package com.andes.bpo.ms_siniestros.controller;

import com.andes.bpo.ms_siniestros.model.AreaSiniestro;
import com.andes.bpo.ms_siniestros.service.SiniestroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Tag(name = "1. Catálogo de Áreas", description = "Gestión de rubros y áreas de siniestros (Vehículos, Hogar, Salud, etc.)")
public class CatalogoController {
    private final SiniestroService siniestroService;

    public CatalogoController(SiniestroService siniestroService) {
        this.siniestroService = siniestroService;
    }

    @Operation(summary = "Listar áreas de siniestro", description = "Obtiene todas las áreas o rubros disponibles para clasificar un denuncio.")
    @GetMapping({"/api/catalogo/areas", "/api/areas"})
    public ResponseEntity<List<AreaSiniestro>> listarAreas() {
        return ResponseEntity.ok(siniestroService.listarAreas());
    }

    @Operation(summary = "Crear nueva área", description = "Registra un nuevo rubro o área en el catálogo del sistema.")
    @PostMapping({"/api/catalogo/areas", "/api/areas"})
    public ResponseEntity<AreaSiniestro> crearArea(@RequestBody AreaSiniestro area) {
        return ResponseEntity.ok(siniestroService.crearArea(area));
    }
}