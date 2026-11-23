package com.andes.bpo.ms_siniestros.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LiquidadorDTO {
    private Integer idUsuario;
    private String rut;
    private String nombre;
    private String apellido;
    private String correo;
    private String telefono;
    private String aseguradora;
    private String rol;
    private String equipo;
    private String tipoLiquidador;
    private Integer idArea;
}