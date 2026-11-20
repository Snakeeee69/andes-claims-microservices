package com.andes.bpo.ms_auth_usuarios.config;

import com.andes.bpo.ms_auth_usuarios.model.Credencial;
import com.andes.bpo.ms_auth_usuarios.repository.CredencialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthSeeder implements CommandLineRunner {

    private final CredencialRepository credencialRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String hashBase = passwordEncoder.encode("Andes2026!");
        String hashProvisorio = passwordEncoder.encode("123456");

        List<Credencial> iniciales = List.of(
                Credencial.builder().idUsuario(1).rut("158423715").passwordHash(hashBase).requiereCambioClave(false).build(),
                Credencial.builder().idUsuario(2).rut("761234560").passwordHash(hashBase).requiereCambioClave(false).build(),
                Credencial.builder().idUsuario(3).rut("222222222").passwordHash(hashBase).requiereCambioClave(false).build(),
                Credencial.builder().idUsuario(4).rut("333333333").passwordHash(hashBase).requiereCambioClave(false).build(),
                Credencial.builder().idUsuario(5).rut("444444444").passwordHash(hashBase).requiereCambioClave(false).build(),
                Credencial.builder().idUsuario(6).rut("555555555").passwordHash(hashProvisorio).requiereCambioClave(true).build()
        );

        for (Credencial c : iniciales) {
            if (!credencialRepository.existsByRut(c.getRut())) {
                credencialRepository.save(c);
            }
        }
    }
}