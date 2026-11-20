package com.andes.bpo.ms_auth_usuarios.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "credenciales")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCredencial;

    @Column(nullable = false, unique = true)
    private Integer idUsuario;

    @Column(nullable = false, unique = true, length = 12)
    private String rut;

    @Column(nullable = false)
    private String passwordHash;

    @Builder.Default
    private Boolean requiereCambioClave = false;

    private String codigoRecuperacionHash;
}