package com.andes.bpo.ms_siniestros.repository;

import com.andes.bpo.ms_siniestros.model.LiquidacionGestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LiquidacionRepository extends JpaRepository<LiquidacionGestion, Long> {
    Optional<LiquidacionGestion> findBySiniestroId(Long siniestroId);
    List<LiquidacionGestion> findByRutLiquidadorActualOrderByFechaAsignacionDesc(String rutLiquidadorActual);
}