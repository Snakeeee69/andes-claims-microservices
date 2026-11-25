package com.andes.bpo.ms_usuarios.controller;

import com.andes.bpo.ms_usuarios.dto.ActualizarUsuarioRequest;
import com.andes.bpo.ms_usuarios.dto.RegistroUsuarioRequest;
import com.andes.bpo.ms_usuarios.model.Usuario;
import com.andes.bpo.ms_usuarios.service.UsuarioService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioControllerTest {

    private UsuarioService servicio;
    private UsuarioController controller;

    @BeforeEach
    void preparar() {
        servicio = mock(UsuarioService.class);
        controller = new UsuarioController(servicio);
    }

    private Usuario usuario() {
        return Usuario.builder()
                .idUsuario(7)
                .rut("12345678K")
                .nombre("Ana")
                .apellido("Perez")
                .correo("ana@correo.cl")
                .rol("ASEGURADO_NATURAL")
                .build();
    }

    @Test
    void registrarUsuarioResponde201() {
        RegistroUsuarioRequest req =
                new RegistroUsuarioRequest(
                        "12345678K", "Ana", "Perez", null,
                        "ana@correo.cl", null, null, null,
                        "Clave1234", null, null, null, null
                );

        when(servicio.registrarUsuario(req))
                .thenReturn(usuario());

        var respuesta = controller.registrarUsuario(req);

        assertEquals(HttpStatus.CREATED,
                respuesta.getStatusCode());
        assertEquals("12345678K",
                respuesta.getBody().getRut());

        verify(servicio).registrarUsuario(req);
    }

    @Test
    void obtenerPorRutDevuelveUsuario() {
        when(servicio.obtenerPorRut("12345678K"))
                .thenReturn(usuario());

        var respuesta = controller.obtenerPorRut("12345678K");

        assertEquals(HttpStatus.OK,
                respuesta.getStatusCode());
        assertEquals(7,
                respuesta.getBody().getIdUsuario());
    }

    @Test
    void obtenerPorIdDevuelveUsuario() {
        when(servicio.obtenerPorId(7))
                .thenReturn(usuario());

        var respuesta = controller.obtenerPorId(7);

        assertEquals("Ana",
                respuesta.getBody().getNombre());

        verify(servicio).obtenerPorId(7);
    }

    @Test
    void actualizarUsuarioDevuelvePerfilActualizado() {
        ActualizarUsuarioRequest req =
                new ActualizarUsuarioRequest(
                        "Ana", "Perez", null,
                        "ana@correo.cl", null, null,
                        null, null, null, null, null
                );

        when(servicio.actualizarUsuario(7, req))
                .thenReturn(usuario());

        var respuesta = controller.actualizarUsuario(7, req);

        assertEquals(HttpStatus.OK,
                respuesta.getStatusCode());

        verify(servicio).actualizarUsuario(7, req);
    }

    @Test
    void listarUsuariosDevuelveLista() {
        when(servicio.listarTodos())
                .thenReturn(List.of(usuario()));

        var respuesta = controller.listarUsuarios();

        assertEquals(1, respuesta.getBody().size());

        verify(servicio).listarTodos();
    }

    @Test
    void listarLiquidadoresPorEquipo() {
        when(servicio.listarLiquidadoresPorEquipo("VEHICULOS"))
                .thenReturn(List.of(usuario()));

        var respuesta =
                controller.listarLiquidadoresPorEquipo("VEHICULOS");

        assertEquals(1, respuesta.getBody().size());

        verify(servicio)
                .listarLiquidadoresPorEquipo("VEHICULOS");
    }
}
