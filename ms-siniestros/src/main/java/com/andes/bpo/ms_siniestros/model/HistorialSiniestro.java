package com.andes.bpo.ms_siniestros.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_siniestro")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class HistorialSiniestro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idHistorial;

    @Column(nullable = false)
    private Long siniestroId;

    @Column(nullable = false, length = 80)
    private String modulo;

    @Column(nullable = false, length = 80)
    private String accion;

    @Column(length = 500)
    private String detalle;

    @Column(length = 50)
    private String usuarioResponsable;

    private LocalDateTime fechaRegistro;
}