package com.andes.bpo.ms_siniestros.service;

import com.andes.bpo.ms_siniestros.dto.LiquidadorDTO;
import com.andes.bpo.ms_siniestros.model.LiquidacionGestion;
import com.andes.bpo.ms_siniestros.model.Siniestro;
import com.andes.bpo.ms_siniestros.repository.LiquidacionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class LiquidacionService {

    private final LiquidacionRepository repo;
    private final SiniestroService siniestroService;
    private final RestTemplate restTemplate;

    public LiquidacionService(LiquidacionRepository repo,
                              SiniestroService siniestroService,
                              RestTemplate restTemplate) {
        this.repo = repo;
        this.siniestroService = siniestroService;
        this.restTemplate = restTemplate;
    }

    private List<LiquidadorDTO> obtenerUsuariosDesdeMsUsuarios() {
        HttpHeaders headers = new HttpHeaders();
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String auth = request.getHeader("Authorization");
            if (auth != null && !auth.isBlank()) {
                headers.set("Authorization", auth);
            }
        }
        ResponseEntity<LiquidadorDTO[]> response = restTemplate.exchange(
                "http://ms-usuarios/api/usuarios",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                LiquidadorDTO[].class
        );
        return response.getBody() != null ? Arrays.asList(response.getBody()) : List.of();
    }

    public List<LiquidadorDTO> listarLiquidadores(String tipo, Integer idArea) {
        return obtenerUsuariosDesdeMsUsuarios().stream()
                .filter(u -> u.getRol() != null && u.getRol().toUpperCase().contains("LIQUIDADOR"))
                .filter(u -> tipo == null || tipo.isBlank() || tipo.equalsIgnoreCase(u.getTipoLiquidador()))
                .filter(u -> idArea == null || idArea.equals(u.getIdArea()))
                .toList();
    }

    public LiquidacionGestion obtenerPorSiniestro(Long siniestroId) {
        LiquidacionGestion gestion = repo.findBySiniestroId(siniestroId)
                .orElseThrow(() -> new IllegalArgumentException("No existe liquidación para el siniestro " + siniestroId));
        verificarSilencioPositivo(gestion);
        return gestion;
    }

    @Transactional
    public LiquidacionGestion asignarCaso(Long siniestroId, String rutLiquidador) {
        Siniestro siniestro = siniestroService.obtenerPorId(siniestroId);
        LiquidadorDTO liquidador = obtenerUsuariosDesdeMsUsuarios().stream()
                .filter(u -> rutLiquidador.equalsIgnoreCase(u.getRut()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Liquidador no encontrado: " + rutLiquidador));

        LiquidacionGestion gestion = repo.findBySiniestroId(siniestroId).orElse(new LiquidacionGestion());
        LocalDateTime ahora = LocalDateTime.now();
        gestion.setSiniestroId(siniestroId);
        gestion.setRutLiquidadorActual(liquidador.getRut());
        gestion.setTipoLiquidadorActual(liquidador.getTipoLiquidador() != null ? liquidador.getTipoLiquidador() : "EXTERNO");
        gestion.setEstado("PENDIENTE_ACEPTACION");
        gestion.setEtapaActual("ASIGNADO");
        gestion.setFechaAsignacion(ahora);
        gestion.setFechaLimiteRespuesta(sumarDiasHabiles(ahora, 2));
        gestion.setMotivoRechazo(null);

        siniestro.setEstado("ASIGNADO");
        siniestro.setRutLiquidadorAsignado(liquidador.getRut());
        siniestroService.guardarSiniestro(siniestro);

        siniestroService.registrarHistorial(siniestroId, "LIQUIDACION", "LIQUIDADOR_ASIGNADO",
                "Asignado a liquidador " + liquidador.getRut() + " (plazo 2 días hábiles)", "ANALISTA");
        siniestroService.crearNotificacion(siniestroId, liquidador.getRut(), "LIQUIDACION",
                "Se te asignó el siniestro " + siniestro.getNumeroSiniestro() + ". Plazo legal: 2 días hábiles.");
        siniestroService.crearNotificacion(siniestroId, siniestro.getRutAsegurado(), "LIQUIDACION",
                "Tu siniestro " + siniestro.getNumeroSiniestro() + " fue derivado a un liquidador.");

        return repo.save(gestion);
    }

    @Transactional
    public LiquidacionGestion aceptarCaso(Long siniestroId, String rutLiquidador) {
        Siniestro siniestro = siniestroService.obtenerPorId(siniestroId);
        LiquidacionGestion gestion = obtenerPorSiniestro(siniestroId);
        validarLiquidador(gestion, rutLiquidador);

        gestion.setEstado("ACEPTADO");
        gestion.setEtapaActual("INSPECCION");
        gestion.setFechaRespuesta(LocalDateTime.now());

        siniestro.setEstado("EN_LIQUIDACION");
        siniestroService.guardarSiniestro(siniestro);

        siniestroService.registrarHistorial(siniestroId, "LIQUIDACION", "CASO_ACEPTADO",
                "Liquidador aceptó el caso e inicia inspección", gestion.getRutLiquidadorActual());
        siniestroService.crearNotificacion(siniestroId, siniestro.getRutAsegurado(), "LIQUIDACION",
                "El liquidador aceptó tu siniestro " + siniestro.getNumeroSiniestro() + ".");

        return repo.save(gestion);
    }

    @Transactional
    public LiquidacionGestion rechazarCaso(Long siniestroId, String rutLiquidador, String motivoRechazo) {
        Siniestro siniestro = siniestroService.obtenerPorId(siniestroId);
        LiquidacionGestion gestion = obtenerPorSiniestro(siniestroId);
        String rutActual = (rutLiquidador != null && !rutLiquidador.isBlank()) ? rutLiquidador : gestion.getRutLiquidadorActual();
        validarLiquidador(gestion, rutActual);

        if (motivoRechazo == null || motivoRechazo.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el motivo del rechazo.");
        }

        LiquidadorDTO respaldo = obtenerUsuariosDesdeMsUsuarios().stream()
                .filter(u -> "LIQUIDADOR_RESPALDO".equalsIgnoreCase(u.getRol()) || "RESPALDO".equalsIgnoreCase(u.getTipoLiquidador()))
                .filter(u -> !u.getRut().equalsIgnoreCase(rutActual))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay Liquidador de Respaldo disponible."));

        gestion.setRutLiquidadorAnterior(rutActual);
        gestion.setRutLiquidadorActual(respaldo.getRut());
        gestion.setTipoLiquidadorActual("RESPALDO");
        gestion.setMotivoRechazo(motivoRechazo);
        gestion.setEstado("DERIVADO_RESPALDO");
        gestion.setEtapaActual("ASIGNADO");
        gestion.setFechaRespuesta(LocalDateTime.now());

        siniestro.setRutLiquidadorAsignado(respaldo.getRut());
        siniestroService.guardarSiniestro(siniestro);

        siniestroService.registrarHistorial(siniestroId, "LIQUIDACION", "DERIVADO_RESPALDO",
                "Rechazado por " + rutActual + " (" + motivoRechazo + "). Derivado a " + respaldo.getRut(), rutActual);
        siniestroService.crearNotificacion(siniestroId, respaldo.getRut(), "LIQUIDACION",
                "Caso " + siniestro.getNumeroSiniestro() + " derivado a respaldo por rechazo del titular.");

        return repo.save(gestion);
    }

    @Transactional
    public LiquidacionGestion actualizarEtapa(Long siniestroId, String etapa) {
        Siniestro siniestro = siniestroService.obtenerPorId(siniestroId);
        LiquidacionGestion gestion = obtenerPorSiniestro(siniestroId);
        gestion.setEstado("EN_PROCESO");
        gestion.setEtapaActual(etapa);

        siniestroService.registrarHistorial(siniestroId, "LIQUIDACION", "AVANCE_ETAPA",
                "Etapa actualizada a " + etapa, gestion.getRutLiquidadorActual());
        siniestroService.crearNotificacion(siniestroId, siniestro.getRutAsegurado(), "LIQUIDACION",
                "Tu siniestro " + siniestro.getNumeroSiniestro() + " avanzó a etapa: " + etapa);

        return repo.save(gestion);
    }

    @Transactional
    public LiquidacionGestion cerrarLiquidacion(Long siniestroId, String rutLiquidador, Double monto, String informe) {
        Siniestro siniestro = siniestroService.obtenerPorId(siniestroId);
        LiquidacionGestion gestion = obtenerPorSiniestro(siniestroId);
        validarLiquidador(gestion, rutLiquidador);

        gestion.setEstado("CERRADO");
        gestion.setEtapaActual("LIQUIDADO");
        gestion.setMontoIndemnizacion(monto);
        gestion.setInformeLiquidacion(informe);
        gestion.setFechaRespuesta(LocalDateTime.now());

        siniestro.setEstado("CERRADO");
        siniestroService.guardarSiniestro(siniestro);

        siniestroService.registrarHistorial(siniestroId, "LIQUIDACION", "LIQUIDACION_CERRADA",
                "Siniestro cerrado. Indemnización: $" + monto, gestion.getRutLiquidadorActual());
        siniestroService.crearNotificacion(siniestroId, siniestro.getRutAsegurado(), "LIQUIDACION",
                "Liquidación finalizada para " + siniestro.getNumeroSiniestro() + ". Monto aprobado: $" + monto);

        return repo.save(gestion);
    }

    public List<LiquidacionGestion> listarCasosPorLiquidador(String rutLiquidador) {
        List<LiquidacionGestion> casos = repo.findByRutLiquidadorActualOrderByFechaAsignacionDesc(rutLiquidador);
        casos.forEach(this::verificarSilencioPositivo);
        return casos;
    }

    private void verificarSilencioPositivo(LiquidacionGestion gestion) {
        if ("PENDIENTE_ACEPTACION".equals(gestion.getEstado())
                && gestion.getFechaLimiteRespuesta() != null
                && LocalDateTime.now().isAfter(gestion.getFechaLimiteRespuesta())) {
            gestion.setEstado("ACEPTADO_SILENCIO_POSITIVO");
            gestion.setEtapaActual("INSPECCION");
            repo.save(gestion);

            Siniestro siniestro = siniestroService.obtenerPorId(gestion.getSiniestroId());
            siniestro.setEstado("EN_LIQUIDACION");
            siniestroService.guardarSiniestro(siniestro);

            siniestroService.registrarHistorial(gestion.getSiniestroId(), "LIQUIDACION", "SILENCIO_POSITIVO",
                    "Plazo de 2 días hábiles vencido: aceptado automáticamente", "SISTEMA");
            siniestroService.crearNotificacion(gestion.getSiniestroId(), gestion.getRutLiquidadorActual(), "LIQUIDACION",
                    "Plazo vencido: caso aceptado por silencio positivo.");
        }
    }

    private void validarLiquidador(LiquidacionGestion gestion, String rutLiquidador) {
        if (rutLiquidador != null && !rutLiquidador.isBlank()
                && !gestion.getRutLiquidadorActual().equalsIgnoreCase(rutLiquidador)) {
            throw new IllegalStateException("Este siniestro está asignado a otro liquidador.");
        }
    }

    private LocalDateTime sumarDiasHabiles(LocalDateTime inicio, int dias) {
        LocalDateTime resultado = inicio;
        int sumados = 0;
        while (sumados < dias) {
            resultado = resultado.plusDays(1);
            DayOfWeek dia = resultado.getDayOfWeek();
            if (dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY) {
                sumados++;
            }
        }
        return resultado;
    }
}