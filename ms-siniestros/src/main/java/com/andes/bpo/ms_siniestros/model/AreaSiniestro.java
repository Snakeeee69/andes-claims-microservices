package com.andes.bpo.ms_siniestros.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "areas_siniestro")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AreaSiniestro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idArea;

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 250)
    private String descripcion;
}