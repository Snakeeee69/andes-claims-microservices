package com.andes.bpo.ms_siniestros.repository;

import com.andes.bpo.ms_siniestros.model.DocumentoSiniestro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentoSiniestroRepository extends JpaRepository<DocumentoSiniestro, Long> {
    List<DocumentoSiniestro> findBySiniestroIdOrderByFechaSubidaDesc(Long siniestroId);
}