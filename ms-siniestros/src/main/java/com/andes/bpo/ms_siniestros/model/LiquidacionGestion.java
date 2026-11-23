package com.andes.bpo.ms_siniestros.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "liquidacion_gestiones")
@Data
public class LiquidacionGestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long siniestroId;

    @Column(nullable = false)
    private String rutLiquidadorActual;

    private String tipoLiquidadorActual;

    @Column(nullable = false)
    private String estado;

    private String etapaActual;

    @Column(length = 500)
    private String motivoRechazo;

    private String rutLiquidadorAnterior;
    private Double montoIndemnizacion;

    @Column(length = 1000)
    private String informeLiquidacion;

    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaLimiteRespuesta;
    private LocalDateTime fechaRespuesta;
}