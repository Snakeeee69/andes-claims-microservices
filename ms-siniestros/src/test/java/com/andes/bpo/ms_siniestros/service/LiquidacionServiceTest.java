package com.andes.bpo.ms_siniestros.service;

import com.andes.bpo.ms_siniestros.dto.LiquidadorDTO;
import com.andes.bpo.ms_siniestros.model.LiquidacionGestion;
import com.andes.bpo.ms_siniestros.model.Siniestro;
import com.andes.bpo.ms_siniestros.repository.LiquidacionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LiquidacionServiceTest {

    private LiquidacionRepository repositorio;
    private SiniestroService siniestroService;
    private RestTemplate restTemplate;
    private LiquidacionService servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(LiquidacionRepository.class);
        siniestroService = mock(SiniestroService.class);
        restTemplate = mock(RestTemplate.class);

        servicio = new LiquidacionService(
                repositorio, siniestroService, restTemplate
        );
    }

    private Siniestro siniestro() {
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

    private LiquidacionGestion gestion() {
        LiquidacionGestion g = new LiquidacionGestion();
        g.setSiniestroId(5L);
        g.setRutLiquidadorActual("11111111K");
        g.setTipoLiquidadorActual("EXTERNO");
        g.setEstado("PENDIENTE_ACEPTACION");
        g.setEtapaActual("ASIGNADO");
        return g;
    }

    private LiquidadorDTO liquidador(
            String rut, String rol, String tipo, Integer area
    ) {
        LiquidadorDTO u = new LiquidadorDTO();
        u.setRut(rut);
        u.setRol(rol);
        u.setTipoLiquidador(tipo);
        u.setIdArea(area);
        return u;
    }

    private void simularUsuarios(LiquidadorDTO... usuarios) {
        when(restTemplate.exchange(
                eq("http://ms-usuarios/api/usuarios"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(LiquidadorDTO[].class)
        )).thenReturn(ResponseEntity.ok(usuarios));
    }

    private void simularGestion(LiquidacionGestion g) {
        when(repositorio.findBySiniestroId(5L))
                .thenReturn(Optional.of(g));

        when(repositorio.save(any(LiquidacionGestion.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void listarSoloUsuariosLiquidadores() {
        simularUsuarios(
                liquidador("11111111K", "LIQUIDADOR", "EXTERNO", 1),
                liquidador("22222222K", "ANALISTA", null, 1),
                liquidador("33333333K", "LIQUIDADOR_RESPALDO", "RESPALDO", 2)
        );

        List<LiquidadorDTO> resultado =
                servicio.listarLiquidadores(null, null);

        assertEquals(2, resultado.size());
    }

    @Test
    void filtrarLiquidadoresPorTipoYArea() {
        simularUsuarios(
                liquidador("11111111K", "LIQUIDADOR", "EXTERNO", 1),
                liquidador("22222222K", "LIQUIDADOR", "INTERNO", 1),
                liquidador("33333333K", "LIQUIDADOR", "EXTERNO", 2)
        );

        List<LiquidadorDTO> resultado =
                servicio.listarLiquidadores("EXTERNO", 1);

        assertEquals(1, resultado.size());
        assertEquals("11111111K", resultado.get(0).getRut());
    }

    @Test
    void obtenerLiquidacionInexistente() {
        when(repositorio.findBySiniestroId(5L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.obtenerPorSiniestro(5L));
    }

    @Test
    void obtenerLiquidacionSinPlazoVencido() {
        LiquidacionGestion g = gestion();
        g.setFechaLimiteRespuesta(
                LocalDateTime.now().plusDays(1)
        );
        simularGestion(g);

        LiquidacionGestion resultado =
                servicio.obtenerPorSiniestro(5L);

        assertSame(g, resultado);
        assertEquals("PENDIENTE_ACEPTACION", resultado.getEstado());
        verify(repositorio, never()).save(any());
    }

    @Test
    void asignarLiquidadorInexistente() {
        when(siniestroService.obtenerPorId(5L))
                .thenReturn(siniestro());

        simularUsuarios(
                liquidador("11111111K", "LIQUIDADOR", "EXTERNO", 1)
        );

        assertThrows(IllegalArgumentException.class,
                () -> servicio.asignarCaso(5L, "99999999K"));

        verify(repositorio, never()).save(any());
    }

    @Test
    void asignarCasoActualizaSiniestroYLiquidacion() {
        Siniestro s = siniestro();

        when(siniestroService.obtenerPorId(5L))
                .thenReturn(s);

        when(repositorio.findBySiniestroId(5L))
                .thenReturn(Optional.empty());

        when(repositorio.save(any(LiquidacionGestion.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        simularUsuarios(
                liquidador("11111111K", "LIQUIDADOR", "EXTERNO", 1)
        );

        LiquidacionGestion resultado =
                servicio.asignarCaso(5L, "11111111K");

        assertEquals("PENDIENTE_ACEPTACION",
                resultado.getEstado());
        assertEquals("ASIGNADO", resultado.getEtapaActual());
        assertEquals("11111111K",
                resultado.getRutLiquidadorActual());
        assertEquals("ASIGNADO", s.getEstado());

        assertNotNull(resultado.getFechaAsignacion());
        assertNotNull(resultado.getFechaLimiteRespuesta());

        assertTrue(resultado.getFechaLimiteRespuesta()
                .isAfter(resultado.getFechaAsignacion()));

        DayOfWeek dia = resultado.getFechaLimiteRespuesta()
                .getDayOfWeek();

        assertNotEquals(DayOfWeek.SATURDAY, dia);
        assertNotEquals(DayOfWeek.SUNDAY, dia);

        verify(siniestroService).guardarSiniestro(s);
        verify(siniestroService, times(2))
                .crearNotificacion(eq(5L), anyString(),
                        eq("LIQUIDACION"), anyString());
    }

    @Test
    void aceptarCasoCorrectamente() {
        Siniestro s = siniestro();
        LiquidacionGestion g = gestion();

        when(siniestroService.obtenerPorId(5L))
                .thenReturn(s);
        simularGestion(g);

        LiquidacionGestion resultado =
                servicio.aceptarCaso(5L, "11111111K");

        assertEquals("ACEPTADO", resultado.getEstado());
        assertEquals("INSPECCION", resultado.getEtapaActual());
        assertEquals("EN_LIQUIDACION", s.getEstado());
        assertNotNull(resultado.getFechaRespuesta());

        verify(siniestroService).guardarSiniestro(s);
    }

    @Test
    void impedirAceptacionPorOtroLiquidador() {
        when(siniestroService.obtenerPorId(5L))
                .thenReturn(siniestro());

        simularGestion(gestion());

        assertThrows(IllegalStateException.class,
                () -> servicio.aceptarCaso(5L, "99999999K"));

        verify(repositorio, never()).save(any());
    }

    @Test
    void rechazarSinMotivoProduceError() {
        when(siniestroService.obtenerPorId(5L))
                .thenReturn(siniestro());

        simularGestion(gestion());

        assertThrows(IllegalArgumentException.class,
                () -> servicio.rechazarCaso(
                        5L, "11111111K", ""
                ));
    }

    @Test
    void rechazarSinLiquidadorDeRespaldo() {
        when(siniestroService.obtenerPorId(5L))
                .thenReturn(siniestro());

        simularGestion(gestion());

        simularUsuarios(
                liquidador("11111111K", "LIQUIDADOR", "EXTERNO", 1)
        );

        assertThrows(IllegalStateException.class,
                () -> servicio.rechazarCaso(
                        5L, "11111111K", "Sin disponibilidad"
                ));
    }

    @Test
    void rechazarCasoYDerivarARespaldo() {
        Siniestro s = siniestro();
        LiquidacionGestion g = gestion();

        when(siniestroService.obtenerPorId(5L))
                .thenReturn(s);
        simularGestion(g);

        simularUsuarios(
                liquidador("11111111K", "LIQUIDADOR", "EXTERNO", 1),
                liquidador("22222222K",
                        "LIQUIDADOR_RESPALDO", "RESPALDO", 1)
        );

        LiquidacionGestion resultado =
                servicio.rechazarCaso(
                        5L, "11111111K", "Sin disponibilidad"
                );

        assertEquals("DERIVADO_RESPALDO", resultado.getEstado());
        assertEquals("11111111K",
                resultado.getRutLiquidadorAnterior());
        assertEquals("22222222K",
                resultado.getRutLiquidadorActual());
        assertEquals("22222222K",
                s.getRutLiquidadorAsignado());

        verify(siniestroService).guardarSiniestro(s);
    }

    @Test
    void actualizarEtapaCorrectamente() {
        Siniestro s = siniestro();
        LiquidacionGestion g = gestion();

        when(siniestroService.obtenerPorId(5L))
                .thenReturn(s);
        simularGestion(g);

        LiquidacionGestion resultado =
                servicio.actualizarEtapa(5L, "INFORME");

        assertEquals("EN_PROCESO", resultado.getEstado());
        assertEquals("INFORME", resultado.getEtapaActual());

        verify(siniestroService).registrarHistorial(
                eq(5L), eq("LIQUIDACION"),
                eq("AVANCE_ETAPA"), anyString(), eq("11111111K")
        );
    }

    @Test
    void cerrarLiquidacionCorrectamente() {
        Siniestro s = siniestro();
        LiquidacionGestion g = gestion();

        when(siniestroService.obtenerPorId(5L))
                .thenReturn(s);
        simularGestion(g);

        LiquidacionGestion resultado =
                servicio.cerrarLiquidacion(
                        5L, "11111111K",
                        150000.0, "Informe aprobado"
                );

        assertEquals("CERRADO", resultado.getEstado());
        assertEquals("LIQUIDADO", resultado.getEtapaActual());
        assertEquals(150000.0,
                resultado.getMontoIndemnizacion());
        assertEquals("Informe aprobado",
                resultado.getInformeLiquidacion());
        assertEquals("CERRADO", s.getEstado());

        verify(siniestroService).guardarSiniestro(s);
    }

    @Test
    void impedirCierrePorOtroLiquidador() {
        when(siniestroService.obtenerPorId(5L))
                .thenReturn(siniestro());

        simularGestion(gestion());

        assertThrows(IllegalStateException.class,
                () -> servicio.cerrarLiquidacion(
                        5L, "99999999K",
                        100000.0, "Informe"
                ));
    }

    @Test
    void listarCasosPorLiquidador() {
        when(repositorio
                .findByRutLiquidadorActualOrderByFechaAsignacionDesc(
                        "11111111K"
                ))
                .thenReturn(List.of(gestion()));

        List<LiquidacionGestion> resultado =
                servicio.listarCasosPorLiquidador("11111111K");

        assertEquals(1, resultado.size());
    }

    @Test
    void aceptarAutomaticamentePorPlazoVencido() {
        LiquidacionGestion g = gestion();
        g.setFechaLimiteRespuesta(
                LocalDateTime.now().minusDays(1)
        );

        Siniestro s = siniestro();

        simularGestion(g);

        when(siniestroService.obtenerPorId(5L))
                .thenReturn(s);

        LiquidacionGestion resultado =
                servicio.obtenerPorSiniestro(5L);

        assertEquals("ACEPTADO_SILENCIO_POSITIVO",
                resultado.getEstado());
        assertEquals("INSPECCION", resultado.getEtapaActual());
        assertEquals("EN_LIQUIDACION", s.getEstado());

        verify(repositorio).save(g);
        verify(siniestroService).guardarSiniestro(s);
    }
}
