package com.andes.bpo.ms_siniestros.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "siniestros")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Siniestro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSiniestro;

    @Column(nullable = false, unique = true, length = 30)
    private String numeroSiniestro;

    @Column(nullable = false, length = 12)
    private String rutAsegurado;

    @Column(nullable = false, length = 150)
    private String nombreAsegurado;

    @Column(nullable = false, length = 50)
    private String polizaNumero;

    @Column(nullable = false)
    private Integer idArea;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    private LocalDate fechaOcurrencia;
    private LocalDateTime fechaDenuncio;

    @Column(nullable = false, length = 40)
    private String estado;

    private Double montoReclamado;

    @Column(length = 12)
    private String rutAnalistaAsignado;

    @Column(length = 12)
    private String rutLiquidadorAsignado;
}