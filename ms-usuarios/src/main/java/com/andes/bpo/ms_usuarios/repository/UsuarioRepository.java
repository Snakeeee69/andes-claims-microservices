package com.andes.bpo.ms_usuarios.repository;


import com.andes.bpo.ms_usuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByRut(String rut);
    boolean existsByRut(String rut);
    boolean existsByCorreoIgnoreCase(String correo);
    boolean existsByCorreoIgnoreCaseAndIdUsuarioNot(String correo, Integer idUsuario);
    List<Usuario> findByEquipoAndRolStartingWith(String equipo, String rolPrefix);
}
