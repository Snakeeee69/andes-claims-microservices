package com.andes.bpo.ms_usuarios.config;

import com.andes.bpo.ms_usuarios.model.Usuario;
import com.andes.bpo.ms_usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UsuarioSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;

    @Override
    public void run(String... args) {
        List<Usuario> base = List.of(
                Usuario.builder().rut("158423715").nombre("Camila").apellido("Soto")
                        .fechaNacimiento(LocalDate.of(1992, 5, 14))
                        .correo("camila.soto@correo.cl").telefono("+56987654321")
                        .direccion("Av. Providencia 1840, Providencia")
                        .aseguradora("Sura Seguros").rol("ASEGURADO_NATURAL").build(),
                Usuario.builder().rut("761234560").nombre("Transportes Pacífico").apellido("SpA")
                        .fechaNacimiento(LocalDate.of(2015, 3, 10))
                        .correo("siniestros@tpacifico.cl").telefono("+56224567890")
                        .direccion("Ruta 68 Km 14, Pudahuel")
                        .aseguradora("BCI Seguros").rol("ASEGURADO_EMPRESA").build(),
                Usuario.builder().rut("222222222").nombre("Rodrigo").apellido("Muñoz")
                        .fechaNacimiento(LocalDate.of(1988, 11, 22))
                        .correo("r.munoz@andes.cl").telefono("+56971234567")
                        .direccion("Av. Apoquindo 4500, Las Condes")
                        .aseguradora("Andes BPO").rol("ANALISTA").build(),
                Usuario.builder().rut("333333333").nombre("Admin").apellido("Andes")
                        .fechaNacimiento(LocalDate.of(1985, 1, 15))
                        .correo("admin@andes.cl").telefono("+56961112233")
                        .direccion("Av. Apoquindo 4500, Las Condes")
                        .aseguradora("Andes BPO").rol("ADMINISTRADOR").build(),
                Usuario.builder().rut("444444444").nombre("Javier").apellido("Paredes")
                        .fechaNacimiento(LocalDate.of(1990, 8, 3))
                        .correo("j.paredes@liquidadores.cl").telefono("+56954443322")
                        .direccion("Av. Irarrázaval 2401, Ñuñoa")
                        .aseguradora("Consorcio").rol("LIQUIDADOR_EXTERNO")
                        .equipo("VEHICULOS").tipoLiquidador("EXTERNO").idArea(1).build(),
                Usuario.builder().rut("555555555").nombre("Elena").apellido("Vargas")
                        .fechaNacimiento(LocalDate.of(1994, 9, 9))
                        .correo("e.vargas@liquidadores.cl").telefono("+56943332211")
                        .direccion("Av. Kennedy 5600, Vitacura")
                        .aseguradora("HDI Seguros").rol("LIQUIDADOR_RESPALDO")
                        .equipo("VEHICULOS").tipoLiquidador("RESPALDO").idArea(1).build()
        );

        for (Usuario u : base) {
            if (!usuarioRepository.existsByRut(u.getRut())) {
                usuarioRepository.save(u);
            }
        }
    }
}