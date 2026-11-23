package com.andes.bpo.ms_siniestros.repository;

import com.andes.bpo.ms_siniestros.model.NotificacionSiniestro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificacionSiniestroRepository extends JpaRepository<NotificacionSiniestro, Long> {
    List<NotificacionSiniestro> findBySiniestroIdOrderByFechaCreacionDesc(Long siniestroId);
    List<NotificacionSiniestro> findBySiniestroIdAndMensajeContainingIgnoreCaseOrderByFechaCreacionDesc(Long siniestroId, String palabra);
    List<NotificacionSiniestro> findByRutDestinatarioOrderByFechaCreacionDesc(String rutDestinatario);
}