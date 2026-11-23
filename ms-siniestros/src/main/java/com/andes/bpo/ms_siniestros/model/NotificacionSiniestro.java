package com.andes.bpo.ms_siniestros.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones_siniestro")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificacionSiniestro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotificacion;

    @Column(nullable = false)
    private Long siniestroId;

    @Column(length = 12)
    private String rutDestinatario;

    @Column(nullable = false, length = 50)
    private String tipoEvento;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Builder.Default
    private Boolean leida = false;

    private LocalDateTime fechaCreacion;
}