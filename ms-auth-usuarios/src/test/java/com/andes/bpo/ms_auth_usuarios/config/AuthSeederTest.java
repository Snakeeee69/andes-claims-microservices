package com.andes.bpo.ms_auth_usuarios.config;

import com.andes.bpo.ms_auth_usuarios.model.Credencial;
import com.andes.bpo.ms_auth_usuarios.repository.CredencialRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthSeederTest {

    private CredencialRepository repositorio;
    private PasswordEncoder encoder;
    private AuthSeeder seeder;

    @BeforeEach
    void preparar() {
        repositorio = mock(CredencialRepository.class);
        encoder = mock(PasswordEncoder.class);

        seeder = new AuthSeeder(repositorio, encoder);

        when(encoder.encode("Andes2026!"))
                .thenReturn("hash-principal");

        when(encoder.encode("123456"))
                .thenReturn("hash-provisorio");
    }

    @Test
    void insertarSeisCredencialesCuandoNoExisten() {
        when(repositorio.existsByRut(anyString()))
                .thenReturn(false);

        seeder.run();

        verify(repositorio, times(6))
                .save(any(Credencial.class));
    }

    @Test
    void noInsertarCredencialesDuplicadas() {
        when(repositorio.existsByRut(anyString()))
                .thenReturn(true);

        seeder.run();

        verify(repositorio, never())
                .save(any(Credencial.class));
    }

    @Test
    void insertarSoloCredencialesFaltantes() {
        when(repositorio.existsByRut(anyString()))
                .thenAnswer(inv ->
                        "158423715".equals(inv.getArgument(0))
                );

        seeder.run();

        verify(repositorio, times(5))
                .save(any(Credencial.class));
    }

    @Test
    void credencialProvisoriaRequiereCambioDeClave() {
        when(repositorio.existsByRut(anyString()))
                .thenReturn(false);

        seeder.run();

        ArgumentCaptor<Credencial> captor =
                ArgumentCaptor.forClass(Credencial.class);

        verify(repositorio, times(6))
                .save(captor.capture());

        List<Credencial> credenciales =
                captor.getAllValues();

        Credencial provisoria = credenciales.stream()
                .filter(c -> "555555555".equals(c.getRut()))
                .findFirst()
                .orElseThrow();

        assertTrue(provisoria.getRequiereCambioClave());

        assertEquals(
                "hash-provisorio",
                provisoria.getPasswordHash()
        );
    }
}
