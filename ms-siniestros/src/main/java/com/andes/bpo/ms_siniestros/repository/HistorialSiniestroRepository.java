package com.andes.bpo.ms_siniestros.repository;

import com.andes.bpo.ms_siniestros.model.HistorialSiniestro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialSiniestroRepository extends JpaRepository<HistorialSiniestro, Long> {
    List<HistorialSiniestro> findBySiniestroIdOrderByFechaRegistroDesc(Long siniestroId);
}