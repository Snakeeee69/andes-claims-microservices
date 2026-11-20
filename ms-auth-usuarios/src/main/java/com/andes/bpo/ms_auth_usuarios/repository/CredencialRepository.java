package com.andes.bpo.ms_auth_usuarios.repository;

import com.andes.bpo.ms_auth_usuarios.model.Credencial;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CredencialRepository extends JpaRepository<Credencial, Integer> {
    Optional<Credencial> findByRut(String rut);
    Optional<Credencial> findByIdUsuario(Integer idUsuario);
    boolean existsByRut(String rut);
}