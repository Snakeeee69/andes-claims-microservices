package com.andes.bpo.ms_siniestros.service;

import com.andes.bpo.ms_siniestros.model.*;
import com.andes.bpo.ms_siniestros.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SiniestroServiceTest {

    private SiniestroRepository siniestroRepo;
    private AreaSiniestroRepository areaRepo;
    private AsignacionAnalistaRepository asignacionRepo;
    private DocumentoSiniestroRepository documentoRepo;
    private HistorialSiniestroRepository historialRepo;
    private NotificacionSiniestroRepository notificacionRepo;

    private SiniestroService servicio;

    @BeforeEach
    void preparar() {
        siniestroRepo = mock(SiniestroRepository.class);
        areaRepo = mock(AreaSiniestroRepository.class);
        asignacionRepo = mock(AsignacionAnalistaRepository.class);
        documentoRepo = mock(DocumentoSiniestroRepository.class);
        historialRepo = mock(HistorialSiniestroRepository.class);
        notificacionRepo = mock(NotificacionSiniestroRepository.class);

        servicio = new SiniestroService(
                siniestroRepo, areaRepo,
                asignacionRepo, documentoRepo,
                historialRepo, notificacionRepo
        );
    }

    private Siniestro siniestroDePrueba() {
        return Siniestro.builder()
                .idSiniestro(5L)
                .numeroSiniestro("SIN-PRUEBA")
                .rutAsegurado("12345678K")
                .nombreAsegurado("Usuario Prueba")
                .polizaNumero("POL-001")
                .idArea(1)
                .descripcion("Daño en vehículo")
                .estado("INGRESADO")
                .build();
    }

    @Test
    void cargarAreasCuandoBaseEstaVacia() {
        when(areaRepo.count()).thenReturn(0L);

        servicio.run();

        verify(areaRepo).saveAll(anyList());
    }

    @Test
    void noDuplicarAreasExistentes() {
        when(areaRepo.count()).thenReturn(3L);

        servicio.run();

        verify(areaRepo, never()).saveAll(anyList());
    }

    @Test
    void listarAreasDisponibles() {
        AreaSiniestro area = AreaSiniestro.builder()
                .nombre("Vehículos")
                .build();

        when(areaRepo.findAll()).thenReturn(List.of(area));

        assertEquals(1, servicio.listarAreas().size());
    }

    @Test
    void crearAreaNueva() {
        AreaSiniestro area = AreaSiniestro.builder()
                .nombre("Hogar")
                .build();

        when(areaRepo.save(area)).thenReturn(area);

        assertSame(area, servicio.crearArea(area));
    }

    @Test
    void listarTodosLosSiniestros() {
        when(siniestroRepo.findAll())
                .thenReturn(List.of(siniestroDePrueba()));

        assertEquals(1, servicio.listarTodos().size());
    }

    @Test
    void obtenerSiniestroExistente() {
        Siniestro siniestro = siniestroDePrueba();

        when(siniestroRepo.findById(5L))
                .thenReturn(Optional.of(siniestro));

        assertSame(siniestro, servicio.obtenerPorId(5L));
    }

    @Test
    void obtenerSiniestroInexistente() {
        when(siniestroRepo.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.obtenerPorId(99L));
    }

    @Test
    void listarPorAsegurado() {
        when(siniestroRepo
                .findByRutAseguradoOrderByFechaDenuncioDesc("12345678K"))
                .thenReturn(List.of(siniestroDePrueba()));

        assertEquals(1,
                servicio.listarPorAsegurado("12345678K").size());
    }

    @Test
    void listarPorAnalista() {
        when(siniestroRepo
                .findByRutAnalistaAsignadoOrderByFechaDenuncioDesc("11111111K"))
                .thenReturn(List.of(siniestroDePrueba()));

        assertEquals(1,
                servicio.listarPorAnalista("11111111K").size());
    }

    @Test
    void listarPorLiquidador() {
        when(siniestroRepo
                .findByRutLiquidadorAsignadoOrderByFechaDenuncioDesc("22222222K"))
                .thenReturn(List.of(siniestroDePrueba()));

        assertEquals(1,
                servicio.listarPorLiquidador("22222222K").size());
    }

    @Test
    void crearDenuncioAsignaNumeroFechaYEstado() {
        Siniestro siniestro = siniestroDePrueba();
        siniestro.setEstado(null);

        when(siniestroRepo.save(any(Siniestro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Siniestro resultado = servicio.crearDenuncio(siniestro);

        assertEquals("INGRESADO", resultado.getEstado());
        assertNotNull(resultado.getFechaDenuncio());

        assertTrue(resultado.getNumeroSiniestro()
                .matches("SIN-[A-F0-9]{8}"));

        verify(historialRepo).save(any(HistorialSiniestro.class));
        verify(notificacionRepo).save(any(NotificacionSiniestro.class));
    }

    @Test
    void crearDenuncioConservaEstadoExistente() {
        Siniestro siniestro = siniestroDePrueba();
        siniestro.setEstado("EN_EVALUACION");

        when(siniestroRepo.save(any(Siniestro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Siniestro resultado = servicio.crearDenuncio(siniestro);

        assertEquals("EN_EVALUACION", resultado.getEstado());
    }

    @Test
    void guardarSiniestro() {
        Siniestro siniestro = siniestroDePrueba();

        when(siniestroRepo.save(siniestro))
                .thenReturn(siniestro);

        assertSame(siniestro, servicio.guardarSiniestro(siniestro));
    }

    @Test
    void asignarAnalistaActualizaEstado() {
        Siniestro siniestro = siniestroDePrueba();

        when(siniestroRepo.findById(5L))
                .thenReturn(Optional.of(siniestro));

        when(asignacionRepo.save(any(AsignacionAnalista.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AsignacionAnalista asignacion =
                servicio.asignarAnalista(
                        5L, "11111111K", null, "Asignación inicial"
                );

        assertEquals("EN_EVALUACION", siniestro.getEstado());
        assertEquals("11111111K",
                siniestro.getRutAnalistaAsignado());

        assertEquals("SUPERVISOR",
                asignacion.getAsignadoPor());

        assertNotNull(asignacion.getFechaAsignacion());

        verify(siniestroRepo).save(siniestro);
        verify(historialRepo).save(any(HistorialSiniestro.class));
        verify(notificacionRepo, times(2))
                .save(any(NotificacionSiniestro.class));
    }

    @Test
    void agregarDocumentoPdfRegistraHistorial() {
        Siniestro siniestro = siniestroDePrueba();

        when(siniestroRepo.findById(5L))
                .thenReturn(Optional.of(siniestro));

        when(documentoRepo.save(any(DocumentoSiniestro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentoSiniestro documento =
                DocumentoSiniestro.builder()
                        .nombreArchivo("informe.pdf")
                        .tipoDocumento("PDF")
                        .urlArchivo("archivo.pdf")
                        .build();

        DocumentoSiniestro resultado =
                servicio.agregarDocumento(5L, documento);

        assertEquals(5L, resultado.getSiniestroId());
        assertNotNull(resultado.getFechaSubida());

        verify(historialRepo).save(argThat(h ->
                "DOCUMENTO_ADJUNTADO".equals(h.getAccion()) &&
                h.getDetalle().contains("PDF")
        ));
    }

    @Test
    void agregarDocumentoNotificaResponsables() {
        Siniestro siniestro = siniestroDePrueba();

        siniestro.setRutAnalistaAsignado("11111111K");
        siniestro.setRutLiquidadorAsignado("22222222K");

        when(siniestroRepo.findById(5L))
                .thenReturn(Optional.of(siniestro));

        when(documentoRepo.save(any(DocumentoSiniestro.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DocumentoSiniestro documento =
                DocumentoSiniestro.builder()
                        .nombreArchivo("foto.jpg")
                        .tipoDocumento("IMAGEN")
                        .urlArchivo("foto.jpg")
                        .build();

        servicio.agregarDocumento(5L, documento);

        verify(notificacionRepo, times(3))
                .save(any(NotificacionSiniestro.class));
    }

    @Test
    void listarDocumentosDelSiniestro() {
        when(documentoRepo
                .findBySiniestroIdOrderByFechaSubidaDesc(5L))
                .thenReturn(List.of(
                        DocumentoSiniestro.builder()
                                .nombreArchivo("foto.jpg")
                                .build()
                ));

        assertEquals(1, servicio.listarDocumentos(5L).size());
    }

    @Test
    void listarHistorialDelSiniestro() {
        when(historialRepo
                .findBySiniestroIdOrderByFechaRegistroDesc(5L))
                .thenReturn(List.of(
                        HistorialSiniestro.builder()
                                .accion("DENUNCIO_CREADO")
                                .build()
                ));

        assertEquals(1, servicio.listarHistorial(5L).size());
    }

    @Test
    void registrarHistorialSinResponsableUsaSistema() {
        servicio.registrarHistorial(
                5L, "SINIESTRO", "ACTUALIZACION",
                "Caso actualizado", null
        );

        verify(historialRepo).save(argThat(h ->
                "SISTEMA".equals(h.getUsuarioResponsable()) &&
                "ACTUALIZACION".equals(h.getAccion()) &&
                h.getFechaRegistro() != null
        ));
    }

    @Test
    void listarNotificacionesConFiltro() {
        servicio.listarNotificacionesPorSiniestro(5L, "documento");

        verify(notificacionRepo)
                .findBySiniestroIdAndMensajeContainingIgnoreCaseOrderByFechaCreacionDesc(
                        5L, "documento"
                );
    }

    @Test
    void listarNotificacionesSinFiltro() {
        servicio.listarNotificacionesPorSiniestro(5L, "");

        verify(notificacionRepo)
                .findBySiniestroIdOrderByFechaCreacionDesc(5L);
    }

    @Test
    void listarNotificacionesDeUsuario() {
        servicio.listarNotificacionesPorUsuario("12345678K");

        verify(notificacionRepo)
                .findByRutDestinatarioOrderByFechaCreacionDesc("12345678K");
    }

    @Test
    void marcarNotificacionComoLeida() {
        NotificacionSiniestro notificacion =
                NotificacionSiniestro.builder()
                        .idNotificacion(7L)
                        .leida(false)
                        .build();

        when(notificacionRepo.findById(7L))
                .thenReturn(Optional.of(notificacion));

        when(notificacionRepo.save(notificacion))
                .thenReturn(notificacion);

        NotificacionSiniestro resultado =
                servicio.marcarNotificacionLeida(7L);

        assertTrue(resultado.getLeida());

        verify(notificacionRepo).save(notificacion);
    }

    @Test
    void marcarNotificacionInexistente() {
        when(notificacionRepo.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.marcarNotificacionLeida(99L));
    }

    @Test
    void crearNotificacionCorrectamente() {
        servicio.crearNotificacion(
                5L, "12345678K", "DOCUMENTO",
                "Documento recibido"
        );

        verify(notificacionRepo).save(argThat(n ->
                "12345678K".equals(n.getRutDestinatario()) &&
                "DOCUMENTO".equals(n.getTipoEvento()) &&
                Boolean.FALSE.equals(n.getLeida()) &&
                n.getFechaCreacion() != null
        ));
    }

    @Test
    void listarAsignacionesDelSiniestro() {
        servicio.listarAsignacionesPorSiniestro(5L);

        verify(asignacionRepo)
                .findBySiniestroIdOrderByFechaAsignacionDesc(5L);
    }
}
