package com.andes.bpo.ms_siniestros.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_siniestro")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentoSiniestro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDocumento;

    @Column(nullable = false)
    private Long siniestroId;

    @Column(nullable = false, length = 150)
    private String nombreArchivo;

    @Column(nullable = false, length = 50)
    private String tipoDocumento;

    @Column(nullable = false, length = 500)
    private String urlArchivo;

    private LocalDateTime fechaSubida;
}