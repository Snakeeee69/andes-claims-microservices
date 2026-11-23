package com.andes.bpo.ms_siniestros.repository;

import com.andes.bpo.ms_siniestros.model.AsignacionAnalista;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AsignacionAnalistaRepository extends JpaRepository<AsignacionAnalista, Long> {
    List<AsignacionAnalista> findBySiniestroIdOrderByFechaAsignacionDesc(Long siniestroId);
}