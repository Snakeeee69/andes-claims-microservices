package com.andes.bpo.ms_siniestros.controller;

import com.andes.bpo.ms_siniestros.dto.LiquidadorDTO;
import com.andes.bpo.ms_siniestros.model.*;
import com.andes.bpo.ms_siniestros.service.LiquidacionService;
import com.andes.bpo.ms_siniestros.service.SiniestroService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ControladoresSiniestrosTest {

    private SiniestroService siniestros;
    private LiquidacionService liquidaciones;

    private SiniestroController sc;
    private CatalogoController catalogo;
    private AsignacionController asignaciones;
    private DocumentoController documentos;
    private HistorialController historial;
    private NotificacionController notificaciones;
    private LiquidacionController lc;

    @BeforeEach
    void preparar() {
        siniestros = mock(SiniestroService.class);
        liquidaciones = mock(LiquidacionService.class);

        sc = new SiniestroController(siniestros);
        catalogo = new CatalogoController(siniestros);
        asignaciones = new AsignacionController(siniestros);
        documentos = new DocumentoController(siniestros);
        historial = new HistorialController(siniestros);
        notificaciones = new NotificacionController(siniestros);
        lc = new LiquidacionController(liquidaciones);
    }

    @Test
    void consultarYCrearSiniestros() {
        Siniestro s = mock(Siniestro.class);

        when(siniestros.listarTodos()).thenReturn(List.of(s));
        when(siniestros.obtenerPorId(5L)).thenReturn(s);
        when(siniestros.listarPorAsegurado("111K"))
                .thenReturn(List.of(s));
        when(siniestros.listarPorAnalista("222K"))
                .thenReturn(List.of(s));
        when(siniestros.listarPorLiquidador("333K"))
                .thenReturn(List.of(s));
        when(siniestros.crearDenuncio(s)).thenReturn(s);

        assertEquals(1, sc.listarTodos().getBody().size());
        assertSame(s, sc.obtenerPorId(5L).getBody());
        assertEquals(1, sc.porAsegurado("111K").getBody().size());
        assertEquals(1, sc.porAnalista("222K").getBody().size());
        assertEquals(1, sc.porLiquidador("333K").getBody().size());
        assertSame(s, sc.crear(s).getBody());

        verify(siniestros).crearDenuncio(s);
    }

    @Test
    void consultarYCrearAreas() {
        AreaSiniestro area = mock(AreaSiniestro.class);

        when(siniestros.listarAreas())
                .thenReturn(List.of(area));
        when(siniestros.crearArea(area))
                .thenReturn(area);

        assertEquals(1, catalogo.listarAreas().getBody().size());
        assertSame(area, catalogo.crearArea(area).getBody());
    }

    @Test
    void SubirYConsultarDocumentos() {
        DocumentoSiniestro documento =
                mock(DocumentoSiniestro.class);

        when(siniestros.agregarDocumento(5L, documento))
                .thenReturn(documento);
        when(siniestros.listarDocumentos(5L))
                .thenReturn(List.of(documento));

        assertSame(documento,
                documentos.subir(5L, documento).getBody());

        assertEquals(1,
                documentos.listar(5L).getBody().size());
    }

    @Test
    void consultarHistorial() {
        HistorialSiniestro evento =
                mock(HistorialSiniestro.class);

        when(siniestros.listarHistorial(5L))
                .thenReturn(List.of(evento));

        assertEquals(1, historial.listar(5L).getBody().size());
    }

    @Test
    void asignarAnalistaConValoresPredeterminados() {
        AsignacionAnalista a = mock(AsignacionAnalista.class);

        when(siniestros.asignarAnalista(
                5L, "222K", "SUPERVISOR", ""
        )).thenReturn(a);

        Map<String, Object> body = Map.of(
                "siniestroId", 5,
                "rutAnalista", "222K"
        );

        assertSame(a, asignaciones.asignar(body).getBody());

        assertSame(a,
                asignaciones.asignarPorPath(
                        5L, Map.of("rutAnalista", "222K")
                ).getBody());

        verify(siniestros, times(2))
                .asignarAnalista(5L, "222K", "SUPERVISOR", "");
    }

    @Test
    void asignarAnalistaConDatosCompletos() {
        AsignacionAnalista a = mock(AsignacionAnalista.class);

        when(siniestros.asignarAnalista(
                5L, "222K", "ADMIN", "Asignación inicial"
        )).thenReturn(a);

        Map<String, Object> body = Map.of(
                "siniestroId", 5,
                "rutAnalista", "222K",
                "asignadoPor", "ADMIN",
                "observacion", "Asignación inicial"
        );

        assertSame(a, asignaciones.asignar(body).getBody());

        assertSame(a,
                asignaciones.asignarPorPath(5L, body).getBody());

        verify(siniestros, times(2))
                .asignarAnalista(
                        5L, "222K", "ADMIN", "Asignación inicial"
                );
    }

    @Test
    void listarAsignaciones() {
        AsignacionAnalista a = mock(AsignacionAnalista.class);

        when(siniestros.listarAsignacionesPorSiniestro(5L))
                .thenReturn(List.of(a));

        assertEquals(1,
                asignaciones.listarPorSiniestro(5L)
                        .getBody().size());
    }

    @Test
    void consultarYMarcarNotificaciones() {
        NotificacionSiniestro n =
                mock(NotificacionSiniestro.class);

        when(siniestros.listarNotificacionesPorSiniestro(
                5L, "documento"
        )).thenReturn(List.of(n));

        when(siniestros.listarNotificacionesPorUsuario("111K"))
                .thenReturn(List.of(n));

        when(siniestros.marcarNotificacionLeida(9L))
                .thenReturn(n);

        assertEquals(1,
                notificaciones.buscarPorSiniestro(
                        5L, "documento"
                ).getBody().size());

        assertEquals(1,
                notificaciones.listarPorUsuario("111K")
                        .getBody().size());

        assertSame(n,
                notificaciones.marcarLeida(9L).getBody());
    }

    @Test
    void filtrarLiquidadoresPorAreaYAlias() {
        LiquidadorDTO u = mock(LiquidadorDTO.class);

        when(liquidaciones.listarLiquidadores("EXTERNO", 2))
                .thenReturn(List.of(u));

        assertEquals(1,
                lc.listarLiquidadores("EXTERNO", 2, 3)
                        .getBody().size());

        assertEquals(1,
                lc.listarLiquidadores("EXTERNO", null, 2)
                        .getBody().size());

        verify(liquidaciones, times(2))
                .listarLiquidadores("EXTERNO", 2);
    }

    @Test
    void asignarYConsultarLiquidaciones() {
        LiquidacionGestion g = mock(LiquidacionGestion.class);

        when(liquidaciones.asignarCaso(5L, "333K"))
                .thenReturn(g);
        when(liquidaciones.obtenerPorSiniestro(5L))
                .thenReturn(g);
        when(liquidaciones.listarCasosPorLiquidador("333K"))
                .thenReturn(List.of(g));

        assertSame(g, lc.asignar(Map.of(
                "siniestroId", 5,
                "rutLiquidador", "333K"
        )).getBody());

        assertSame(g, lc.obtenerPorSiniestro(5L).getBody());

        assertEquals(1,
                lc.casosPorLiquidador("333K")
                        .getBody().size());
    }

    @Test
    void aceptarCasoConYSinRut() {
        LiquidacionGestion g = mock(LiquidacionGestion.class);

        when(liquidaciones.aceptarCaso(5L, null))
                .thenReturn(g);
        when(liquidaciones.aceptarCaso(5L, "333K"))
                .thenReturn(g);

        assertSame(g, lc.aceptar(5L, null).getBody());
        assertSame(g, lc.aceptar(5L, Map.of()).getBody());
        assertSame(g, lc.aceptar(
                5L, Map.of("rutLiquidador", "333K")
        ).getBody());

        verify(liquidaciones, times(2))
                .aceptarCaso(5L, null);
        verify(liquidaciones)
                .aceptarCaso(5L, "333K");
    }

    @Test
    void rechazarCasoConYSinMotivo() {
        LiquidacionGestion g = mock(LiquidacionGestion.class);

        when(liquidaciones.rechazarCaso(5L, null, ""))
                .thenReturn(g);

        when(liquidaciones.rechazarCaso(
                5L, "333K", "Sin disponibilidad"
        )).thenReturn(g);

        assertSame(g,
                lc.rechazar(5L, Map.of()).getBody());

        assertSame(g, lc.rechazar(5L, Map.of(
                "rutLiquidador", "333K",
                "motivoRechazo", "Sin disponibilidad"
        )).getBody());

        verify(liquidaciones).rechazarCaso(5L, null, "");
        verify(liquidaciones).rechazarCaso(
                5L, "333K", "Sin disponibilidad"
        );
    }

    @Test
    void actualizarEtapaDeLiquidacion() {
        LiquidacionGestion g = mock(LiquidacionGestion.class);

        when(liquidaciones.actualizarEtapa(5L, "INFORME"))
                .thenReturn(g);

        assertSame(g, lc.cambiarEtapa(
                5L, Map.of("etapa", "INFORME")
        ).getBody());

        verify(liquidaciones)
                .actualizarEtapa(5L, "INFORME");
    }

    @Test
    void cerrarLiquidacionConDatosCompletos() {
        LiquidacionGestion g = mock(LiquidacionGestion.class);

        when(liquidaciones.cerrarLiquidacion(
                5L, "333K", 150000.0, "Informe final"
        )).thenReturn(g);

        assertSame(g, lc.cerrar(5L, Map.of(
                "rutLiquidador", "333K",
                "montoIndemnizacion", 150000,
                "informeLiquidacion", "Informe final"
        )).getBody());

        verify(liquidaciones).cerrarLiquidacion(
                5L, "333K", 150000.0, "Informe final"
        );
    }

    @Test
    void cerrarLiquidacionConValoresPredeterminados() {
        LiquidacionGestion g = mock(LiquidacionGestion.class);

        when(liquidaciones.cerrarLiquidacion(
                5L, null, 0.0, ""
        )).thenReturn(g);

        assertSame(g,
                lc.cerrar(5L, Map.of()).getBody());

        verify(liquidaciones).cerrarLiquidacion(
                5L, null, 0.0, ""
        );
    }
}
