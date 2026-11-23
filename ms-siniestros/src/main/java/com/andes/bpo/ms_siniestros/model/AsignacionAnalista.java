package com.andes.bpo.ms_siniestros.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "asignaciones_analista")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AsignacionAnalista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAsignacion;

    @Column(nullable = false)
    private Long siniestroId;

    @Column(nullable = false, length = 12)
    private String rutAnalista;

    @Column(length = 50)
    private String asignadoPor;

    @Column(length = 300)
    private String observacion;

    private LocalDateTime fechaAsignacion;
}