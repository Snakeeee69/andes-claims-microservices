package com.andes.bpo.ms_usuarios.config;

import com.andes.bpo.ms_usuarios.model.Usuario;
import com.andes.bpo.ms_usuarios.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UsuarioSeederTest {

    private UsuarioRepository repositorio;
    private UsuarioSeeder seeder;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        seeder = new UsuarioSeeder(repositorio);
    }

    @Test
    void insertarUsuariosCuandoNoExisten() {
        when(repositorio.existsByRut(anyString()))
                .thenReturn(false);

        seeder.run();

        verify(repositorio, times(6))
                .save(any(Usuario.class));
    }

    @Test
    void noDuplicarUsuariosExistentes() {
        when(repositorio.existsByRut(anyString()))
                .thenReturn(true);

        seeder.run();

        verify(repositorio, never())
                .save(any(Usuario.class));
    }

    @Test
    void insertarSoloUsuariosFaltantes() {
        when(repositorio.existsByRut(anyString()))
                .thenAnswer(inv -> {
                    String rut = inv.getArgument(0);
                    return "158423715".equals(rut);
                });

        seeder.run();

        verify(repositorio, times(5))
                .save(any(Usuario.class));

        verify(repositorio, never())
                .save(argThat(u ->
                        "158423715".equals(u.getRut())
                ));
    }
}
