package com.andes.bpo.ms_usuarios.service;

import com.andes.bpo.ms_usuarios.dto.ActualizarUsuarioRequest;
import com.andes.bpo.ms_usuarios.dto.RegistroUsuarioRequest;
import com.andes.bpo.ms_usuarios.exception.RecursoNoEncontradoException;
import com.andes.bpo.ms_usuarios.exception.ReglaNegocioException;
import com.andes.bpo.ms_usuarios.model.Usuario;
import com.andes.bpo.ms_usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioServiceTest {

    private UsuarioRepository repositorio;
    private UsuarioService servicio;

    @BeforeEach
    void preparar() throws Exception {
        repositorio = mock(UsuarioRepository.class);
        servicio = crearServicioConRepositorio(repositorio);
    }

    private UsuarioService crearServicioConRepositorio(UsuarioRepository repo) throws Exception {
        for (Constructor<?> ctor : UsuarioService.class.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 1 && UsuarioRepository.class.isAssignableFrom(params[0])) {
                ctor.setAccessible(true);
                return (UsuarioService) ctor.newInstance(repo);
            }
        }
        for (Constructor<?> ctor : UsuarioService.class.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            boolean tieneRepo = false;
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                if (UsuarioRepository.class.isAssignableFrom(params[i])) {
                    args[i] = repo;
                    tieneRepo = true;
                } else if (params[i] == RestClient.Builder.class) {
                    args[i] = mock(RestClient.Builder.class);
                } else if (params[i] == String.class) {
                    args[i] = "http://localhost:8080";
                } else if (!params[i].isPrimitive()) {
                    args[i] = mock(params[i]);
                } else if (params[i] == boolean.class) {
                    args[i] = false;
                } else if (params[i] == int.class || params[i] == long.class || params[i] == double.class) {
                    args[i] = 0;
                }
            }
            if (tieneRepo) {
                ctor.setAccessible(true);
                return (UsuarioService) ctor.newInstance(args);
            }
        }
        throw new IllegalStateException("Constructor no encontrado");
    }

    @Test
    void normalizarRutConFormato() {
        assertEquals("12345678K", servicio.normalizarRut("12.345.678-k"));
    }

    @Test
    void normalizarRutNulo() {
        assertEquals("", servicio.normalizarRut(null));
    }

    @Test
    void obtenerPorRutNormalizaAntesDeConsultar() {
        Usuario usuario = usuarioDePrueba();
        when(repositorio.findByRut("12345678K")).thenReturn(Optional.of(usuario));

        assertSame(usuario, servicio.obtenerPorRut("12.345.678-k"));
        verify(repositorio).findByRut("12345678K");
    }

    @Test
    void obtenerPorRutInexistenteLanzaError() {
        when(repositorio.findByRut("12345678K")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.obtenerPorRut("12.345.678-k"));
    }

    @Test
    void obtenerPorIdExistente() {
        Usuario usuario = usuarioDePrueba();
        when(repositorio.findById(7)).thenReturn(Optional.of(usuario));

        assertSame(usuario, servicio.obtenerPorId(7));
    }

    @Test
    void obtenerPorIdInexistenteLanzaError() {
        when(repositorio.findById(7)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.obtenerPorId(7));
    }

    @Test
    void registrarRutDuplicadoLanzaErrorSinGuardar() {
        when(repositorio.existsByRut("12345678K")).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrarUsuario(registroDePrueba(LocalDate.of(1998, 8, 15))));

        assertEquals("Este RUT ya está registrado", error.getMessage());
        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    void registrarCorreoDuplicadoLanzaErrorSinGuardar() {
        when(repositorio.existsByCorreoIgnoreCase("persona@correo.cl")).thenReturn(true);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrarUsuario(registroDePrueba(LocalDate.of(1998, 8, 15))));

        assertEquals("Este correo ya está registrado", error.getMessage());
        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    void registrarFechaNacimientoFuturaLanzaError() {
        LocalDate manana = LocalDate.now().plusDays(1);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.registrarUsuario(registroDePrueba(manana)));

        assertEquals("La fecha de nacimiento no puede ser futura", error.getMessage());
        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarCorreoDeOtraPersonaLanzaError() {
        Usuario usuario = usuarioDePrueba();
        when(repositorio.findById(7)).thenReturn(Optional.of(usuario));
        when(repositorio.existsByCorreoIgnoreCaseAndIdUsuarioNot("nuevo@correo.cl", 7))
                .thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizarUsuario(7, actualizacionDePrueba()));
        verify(repositorio, never()).save(any(Usuario.class));
    }

    @Test
    void actualizarUsuarioGuardaCamposNormalizados() {
        Usuario usuario = usuarioDePrueba();
        when(repositorio.findById(7)).thenReturn(Optional.of(usuario));
        when(repositorio.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario actualizado = servicio.actualizarUsuario(7, actualizacionDePrueba());

        assertEquals("María", actualizado.getNombre());
        assertEquals("Gómez", actualizado.getApellido());
        assertEquals("nuevo@correo.cl", actualizado.getCorreo());
        assertEquals("ASEGURADO_NATURAL", actualizado.getRol());
        verify(repositorio).save(usuario);
    }

    @Test
    void listarTodosDevuelveResultadosRepositorio() {
        List<Usuario> usuarios = List.of(usuarioDePrueba());
        when(repositorio.findAll()).thenReturn(usuarios);

        assertSame(usuarios, servicio.listarTodos());
    }

    @Test
    void listarLiquidadoresConvierteEquipoAMayusculas() {
        List<Usuario> liquidadores = List.of(usuarioDePrueba());
        when(repositorio.findByEquipoAndRolStartingWith("NORTE", "LIQUIDADOR"))
                .thenReturn(liquidadores);

        assertSame(liquidadores, servicio.listarLiquidadoresPorEquipo("norte"));
        verify(repositorio).findByEquipoAndRolStartingWith("NORTE", "LIQUIDADOR");
    }

    private Usuario usuarioDePrueba() {
        return Usuario.builder()
                .idUsuario(7)
                .rut("12345678K")
                .nombre("Persona")
                .apellido("Prueba")
                .correo("persona@correo.cl")
                .rol("ASEGURADO_NATURAL")
                .build();
    }

    private RegistroUsuarioRequest registroDePrueba(LocalDate nacimiento) {
        return new RegistroUsuarioRequest(
                "12.345.678-k", "Persona", "Prueba", nacimiento,
                "Persona@Correo.cl", null, null, null,
                "Clave1234", "ASEGURADO_NATURAL", null, null, null
        );
    }

    private ActualizarUsuarioRequest actualizacionDePrueba() {
        return new ActualizarUsuarioRequest(
                " María ", " Gómez ", null, " NUEVO@CORREO.CL ",
                null, null, null, "ASEGURADO_NATURAL", null, null, null
        );
    }
}