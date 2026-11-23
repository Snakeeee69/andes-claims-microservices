package com.andes.bpo.ms_siniestros.service;

import com.andes.bpo.ms_siniestros.model.*;
import com.andes.bpo.ms_siniestros.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SiniestroService implements CommandLineRunner {

    private final SiniestroRepository siniestroRepo;
    private final AreaSiniestroRepository areaRepo;
    private final AsignacionAnalistaRepository asignacionRepo;
    private final DocumentoSiniestroRepository documentoRepo;
    private final HistorialSiniestroRepository historialRepo;
    private final NotificacionSiniestroRepository notificacionRepo;

    public SiniestroService(SiniestroRepository siniestroRepo,
                            AreaSiniestroRepository areaRepo,
                            AsignacionAnalistaRepository asignacionRepo,
                            DocumentoSiniestroRepository documentoRepo,
                            HistorialSiniestroRepository historialRepo,
                            NotificacionSiniestroRepository notificacionRepo) {
        this.siniestroRepo = siniestroRepo;
        this.areaRepo = areaRepo;
        this.asignacionRepo = asignacionRepo;
        this.documentoRepo = documentoRepo;
        this.historialRepo = historialRepo;
        this.notificacionRepo = notificacionRepo;
    }

    @Override
    public void run(String... args) {
        if (areaRepo.count() == 0) {
            areaRepo.saveAll(List.of(
                    AreaSiniestro.builder().nombre("Vehículos").descripcion("Colisiones, robos y daños automotrices").build(),
                    AreaSiniestro.builder().nombre("Hogar e Incendio").descripcion("Siniestros estructurales, robo e incendio").build(),
                    AreaSiniestro.builder().nombre("Salud y Vida").descripcion("Reembolsos médicos y accidentes personales").build()
            ));
        }
    }

    // --- CATÁLOGO ---
    public List<AreaSiniestro> listarAreas() { return areaRepo.findAll(); }
    public AreaSiniestro crearArea(AreaSiniestro area) { return areaRepo.save(area); }

    // --- SINIESTROS ---
    public List<Siniestro> listarTodos() { return siniestroRepo.findAll(); }

    public Siniestro obtenerPorId(Long id) {
        return siniestroRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Siniestro no encontrado: " + id));
    }

    public List<Siniestro> listarPorAsegurado(String rut) { return siniestroRepo.findByRutAseguradoOrderByFechaDenuncioDesc(rut); }
    public List<Siniestro> listarPorAnalista(String rut) { return siniestroRepo.findByRutAnalistaAsignadoOrderByFechaDenuncioDesc(rut); }
    public List<Siniestro> listarPorLiquidador(String rut) { return siniestroRepo.findByRutLiquidadorAsignadoOrderByFechaDenuncioDesc(rut); }

    @Transactional
    public Siniestro crearDenuncio(Siniestro siniestro) {
        siniestro.setNumeroSiniestro("SIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        siniestro.setFechaDenuncio(LocalDateTime.now());
        if (siniestro.getEstado() == null || siniestro.getEstado().isBlank()) {
            siniestro.setEstado("INGRESADO");
        }
        Siniestro guardado = siniestroRepo.save(siniestro);

        registrarHistorial(guardado.getIdSiniestro(), "SINIESTRO", "DENUNCIO_CREADO",
                "Ingreso inicial del siniestro " + guardado.getNumeroSiniestro(), guardado.getRutAsegurado());
        crearNotificacion(guardado.getIdSiniestro(), guardado.getRutAsegurado(), "DENUNCIO",
                "Se ha registrado tu denuncio " + guardado.getNumeroSiniestro() + " en estado INGRESADO.");

        return guardado;
    }

    @Transactional
    public Siniestro guardarSiniestro(Siniestro siniestro) {
        return siniestroRepo.save(siniestro);
    }

    // --- ASIGNACIÓN DE ANALISTA ---
    @Transactional
    public AsignacionAnalista asignarAnalista(Long siniestroId, String rutAnalista, String asignadoPor, String observacion) {
        Siniestro siniestro = obtenerPorId(siniestroId);
        siniestro.setRutAnalistaAsignado(rutAnalista);
        siniestro.setEstado("EN_EVALUACION");
        siniestroRepo.save(siniestro);

        AsignacionAnalista asignacion = asignacionRepo.save(AsignacionAnalista.builder()
                .siniestroId(siniestroId)
                .rutAnalista(rutAnalista)
                .asignadoPor(asignadoPor != null ? asignadoPor : "SUPERVISOR")
                .observacion(observacion)
                .fechaAsignacion(LocalDateTime.now())
                .build());

        registrarHistorial(siniestroId, "ASIGNACION", "ANALISTA_ASIGNADO",
                "Analista " + rutAnalista + " asignado al siniestro " + siniestro.getNumeroSiniestro(), asignadoPor);
        crearNotificacion(siniestroId, rutAnalista, "ANALISTA",
                "Se te ha asignado el siniestro " + siniestro.getNumeroSiniestro() + " para evaluación.");
        crearNotificacion(siniestroId, siniestro.getRutAsegurado(), "ANALISTA",
                "Tu siniestro " + siniestro.getNumeroSiniestro() + " pasó a EN_EVALUACION con un analista.");

        return asignacion;
    }

    public List<AsignacionAnalista> listarAsignacionesPorSiniestro(Long siniestroId) {
        return asignacionRepo.findBySiniestroIdOrderByFechaAsignacionDesc(siniestroId);
    }

    // --- DOCUMENTOS ---
    @Transactional
    public DocumentoSiniestro agregarDocumento(Long siniestroId, DocumentoSiniestro doc) {
        Siniestro siniestro = obtenerPorId(siniestroId);
        doc.setSiniestroId(siniestroId);
        doc.setFechaSubida(LocalDateTime.now());
        DocumentoSiniestro guardado = documentoRepo.save(doc);

        String nombreLower = guardado.getNombreArchivo() != null ? guardado.getNombreArchivo().toLowerCase() : "";
        String tipoLower = guardado.getTipoDocumento() != null ? guardado.getTipoDocumento().toLowerCase() : "";

        String mensaje = (nombreLower.contains("pdf") || tipoLower.contains("pdf"))
                ? "Se ha añadido PDF: " + guardado.getNombreArchivo() + " al siniestro " + siniestro.getNumeroSiniestro()
                : "Se ha añadido documento (" + guardado.getTipoDocumento() + "): " + guardado.getNombreArchivo();

        registrarHistorial(siniestroId, "DOCUMENTOS", "DOCUMENTO_ADJUNTADO", mensaje, "USUARIO");
        crearNotificacion(siniestroId, siniestro.getRutAsegurado(), "DOCUMENTO", mensaje);

        if (siniestro.getRutAnalistaAsignado() != null && !siniestro.getRutAnalistaAsignado().isBlank()) {
            crearNotificacion(siniestroId, siniestro.getRutAnalistaAsignado(), "DOCUMENTO", mensaje);
        }
        if (siniestro.getRutLiquidadorAsignado() != null && !siniestro.getRutLiquidadorAsignado().isBlank()) {
            crearNotificacion(siniestroId, siniestro.getRutLiquidadorAsignado(), "DOCUMENTO", mensaje);
        }

        return guardado;
    }

    public List<DocumentoSiniestro> listarDocumentos(Long siniestroId) {
        return documentoRepo.findBySiniestroIdOrderByFechaSubidaDesc(siniestroId);
    }

    // --- HISTORIAL ---
    public List<HistorialSiniestro> listarHistorial(Long siniestroId) {
        return historialRepo.findBySiniestroIdOrderByFechaRegistroDesc(siniestroId);
    }

    public void registrarHistorial(Long siniestroId, String modulo, String accion, String detalle, String responsable) {
        historialRepo.save(HistorialSiniestro.builder()
                .siniestroId(siniestroId)
                .modulo(modulo)
                .accion(accion)
                .detalle(detalle)
                .usuarioResponsable(responsable != null ? responsable : "SISTEMA")
                .fechaRegistro(LocalDateTime.now())
                .build());
    }

    // --- NOTIFICACIONES ---
    public List<NotificacionSiniestro> listarNotificacionesPorSiniestro(Long siniestroId, String palabra) {
        if (palabra != null && !palabra.isBlank()) {
            return notificacionRepo.findBySiniestroIdAndMensajeContainingIgnoreCaseOrderByFechaCreacionDesc(siniestroId, palabra);
        }
        return notificacionRepo.findBySiniestroIdOrderByFechaCreacionDesc(siniestroId);
    }

    public List<NotificacionSiniestro> listarNotificacionesPorUsuario(String rut) {
        return notificacionRepo.findByRutDestinatarioOrderByFechaCreacionDesc(rut);
    }

    @Transactional
    public NotificacionSiniestro marcarNotificacionLeida(Long idNotificacion) {
        NotificacionSiniestro notif = notificacionRepo.findById(idNotificacion)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada: " + idNotificacion));
        notif.setLeida(true);
        return notificacionRepo.save(notif);
    }

    public void crearNotificacion(Long siniestroId, String rutDestinatario, String tipoEvento, String mensaje) {
        notificacionRepo.save(NotificacionSiniestro.builder()
                .siniestroId(siniestroId)
                .rutDestinatario(rutDestinatario)
                .tipoEvento(tipoEvento)
                .mensaje(mensaje)
                .leida(false)
                .fechaCreacion(LocalDateTime.now())
                .build());
    }
}