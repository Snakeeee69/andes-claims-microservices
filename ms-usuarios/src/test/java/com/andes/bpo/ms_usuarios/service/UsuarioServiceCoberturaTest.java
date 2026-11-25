package com.andes.bpo.ms_usuarios.service;

import com.andes.bpo.ms_usuarios.dto.ActualizarUsuarioRequest;
import com.andes.bpo.ms_usuarios.dto.RegistroCredencialRemoto;
import com.andes.bpo.ms_usuarios.dto.RegistroUsuarioRequest;
import com.andes.bpo.ms_usuarios.exception.ReglaNegocioException;
import com.andes.bpo.ms_usuarios.model.Usuario;
import com.andes.bpo.ms_usuarios.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UsuarioServiceCoberturaTest {

    private UsuarioRepository repositorio;
    private UsuarioService servicio;
    private RestClient.RequestBodyUriSpec postSpec;
    private RestClient.RequestBodySpec bodySpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void preparar() throws Exception {
        repositorio = mock(UsuarioRepository.class);
        servicio = crearServicioConRepositorio(repositorio);

        RestClient cliente = mock(RestClient.class);
        postSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(cliente.post()).thenReturn(postSpec);
        when(postSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON))
                .thenReturn(bodySpec);
        when(bodySpec.body(any(RegistroCredencialRemoto.class)))
                .thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenReturn(ResponseEntity.noContent().build());

        ReflectionTestUtils.setField(
                servicio, "restClient", cliente
        );
        ReflectionTestUtils.setField(
                servicio, "authUri", "http://auth-pruebas"
        );

        when(repositorio.save(any(Usuario.class)))
                .thenAnswer(inv -> {
                    Usuario u = inv.getArgument(0);
                    u.setIdUsuario(7);
                    return u;
                });
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
    void registrarUsuarioCompletoNormalizaDatos() {
        RegistroUsuarioRequest solicitud =
                new RegistroUsuarioRequest(
                        "12.345.678-k",
                        " Ana ",
                        " Pérez ",
                        LocalDate.of(1998, 8, 15),
                        " ANA@CORREO.CL ",
                        " +56912345678 ",
                        " Calle 123 ",
                        " BCI Seguros ",
                        "Clave1234",
                        "LIQUIDADOR",
                        "NORTE",
                        "INTERNO",
                        2
                );

        Usuario resultado = servicio.registrarUsuario(solicitud);

        assertEquals(7, resultado.getIdUsuario());
        assertEquals("12345678K", resultado.getRut());
        assertEquals("Ana", resultado.getNombre());
        assertEquals("Pérez", resultado.getApellido());
        assertEquals("ana@correo.cl", resultado.getCorreo());
        assertEquals("+56912345678", resultado.getTelefono());
        assertEquals("Calle 123", resultado.getDireccion());
        assertEquals("BCI Seguros", resultado.getAseguradora());
        assertEquals("LIQUIDADOR", resultado.getRol());
        assertEquals("NORTE", resultado.getEquipo());
        assertEquals("INTERNO", resultado.getTipoLiquidador());
        assertEquals(2, resultado.getIdArea());

        verify(repositorio).save(any(Usuario.class));
        verify(postSpec).uri(
                "http://auth-pruebas/api/auth/interno/credenciales"
        );

        ArgumentCaptor<RegistroCredencialRemoto> captor =
                ArgumentCaptor.forClass(RegistroCredencialRemoto.class);

        verify(bodySpec).body(captor.capture());

        assertEquals("12345678K", captor.getValue().rut());
        assertEquals("Clave1234", captor.getValue().clavePlana());
        assertFalse(captor.getValue().requiereCambioClave());
    }

    @Test
    void registrarSinOpcionalesUtilizaValoresPorDefecto() {
        RegistroUsuarioRequest solicitud =
                new RegistroUsuarioRequest(
                        "12345678K",
                        "Ana",
                        "Perez",
                        null,
                        "ana@correo.cl",
                        null,
                        null,
                        null,
                        null,
                        "",
                        null,
                        null,
                        null
                );

        Usuario resultado = servicio.registrarUsuario(solicitud);

        assertEquals("ASEGURADO_NATURAL", resultado.getRol());
        assertNull(resultado.getTelefono());
        assertNull(resultado.getDireccion());
        assertNull(resultado.getAseguradora());

        ArgumentCaptor<RegistroCredencialRemoto> captor =
                ArgumentCaptor.forClass(RegistroCredencialRemoto.class);

        verify(bodySpec).body(captor.capture());

        assertEquals("123456", captor.getValue().clavePlana());
        assertTrue(captor.getValue().requiereCambioClave());
    }

    @Test
    void falloDeAutenticacionRevierteRegistroLocal() {
        RegistroUsuarioRequest solicitud =
                new RegistroUsuarioRequest(
                        "12345678K",
                        "Ana",
                        "Perez",
                        null,
                        "ana@correo.cl",
                        null,
                        null,
                        null,
                        "Clave1234",
                        null,
                        null,
                        null,
                        null
                );

        when(responseSpec.toBodilessEntity())
                .thenThrow(new RuntimeException("Servicio no disponible"));

        ReglaNegocioException error =
                assertThrows(
                        ReglaNegocioException.class,
                        () -> servicio.registrarUsuario(solicitud)
                );

        assertTrue(error.getMessage().contains("sincronizar"));

        ArgumentCaptor<Usuario> captor =
                ArgumentCaptor.forClass(Usuario.class);

        verify(repositorio).delete(captor.capture());
        assertEquals(7, captor.getValue().getIdUsuario());
    }

    @Test
    void actualizarTodosLosCamposOpcionales() {
        Usuario usuario = Usuario.builder()
                .idUsuario(7)
                .nombre("Original")
                .apellido("Prueba")
                .correo("original@correo.cl")
                .rol("ASEGURADO_NATURAL")
                .build();

        when(repositorio.findById(7))
                .thenReturn(Optional.of(usuario));

        ActualizarUsuarioRequest solicitud =
                new ActualizarUsuarioRequest(
                        " Nueva ",
                        " Persona ",
                        LocalDate.of(1999, 3, 10),
                        " NUEVA@CORREO.CL ",
                        " +56987654321 ",
                        " Avenida 456 ",
                        " Sura Seguros ",
                        "LIQUIDADOR",
                        "SUR",
                        "EXTERNO",
                        3
                );

        Usuario resultado =
                servicio.actualizarUsuario(7, solicitud);

        assertEquals("Nueva", resultado.getNombre());
        assertEquals("Persona", resultado.getApellido());
        assertEquals("nueva@correo.cl", resultado.getCorreo());
        assertEquals(LocalDate.of(1999, 3, 10),
                resultado.getFechaNacimiento());
        assertEquals("+56987654321", resultado.getTelefono());
        assertEquals("Avenida 456", resultado.getDireccion());
        assertEquals("Sura Seguros", resultado.getAseguradora());
        assertEquals("LIQUIDADOR", resultado.getRol());
        assertEquals("SUR", resultado.getEquipo());
        assertEquals("EXTERNO", resultado.getTipoLiquidador());
        assertEquals(3, resultado.getIdArea());

        verify(repositorio).save(usuario);
    }

    @Test
    void actualizarSinOpcionalesConservaDatosOriginales() {
        Usuario usuario = Usuario.builder()
                .idUsuario(7)
                .nombre("Ana")
                .apellido("Perez")
                .correo("ana@correo.cl")
                .rol("LIQUIDADOR")
                .equipo("NORTE")
                .tipoLiquidador("INTERNO")
                .idArea(2)
                .build();

        when(repositorio.findById(7))
                .thenReturn(Optional.of(usuario));

        ActualizarUsuarioRequest solicitud =
                new ActualizarUsuarioRequest(
                        "Ana",
                        "Perez",
                        null,
                        "ana@correo.cl",
                        null,
                        null,
                        null,
                        " ",
                        null,
                        null,
                        null
                );

        Usuario resultado =
                servicio.actualizarUsuario(7, solicitud);

        assertEquals("LIQUIDADOR", resultado.getRol());
        assertEquals("NORTE", resultado.getEquipo());
        assertEquals("INTERNO", resultado.getTipoLiquidador());
        assertEquals(2, resultado.getIdArea());
    }
}