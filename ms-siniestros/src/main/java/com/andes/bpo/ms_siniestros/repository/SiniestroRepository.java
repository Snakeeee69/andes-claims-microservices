package com.andes.bpo.ms_siniestros.repository;

import com.andes.bpo.ms_siniestros.model.Siniestro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SiniestroRepository extends JpaRepository<Siniestro, Long> {
    List<Siniestro> findByRutAseguradoOrderByFechaDenuncioDesc(String rutAsegurado);
    List<Siniestro> findByRutAnalistaAsignadoOrderByFechaDenuncioDesc(String rutAnalistaAsignado);
    List<Siniestro> findByRutLiquidadorAsignadoOrderByFechaDenuncioDesc(String rutLiquidadorAsignado);
}