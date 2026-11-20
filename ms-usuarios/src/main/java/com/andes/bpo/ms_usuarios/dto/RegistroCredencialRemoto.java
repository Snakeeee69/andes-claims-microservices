package com.andes.bpo.ms_usuarios.dto;


public record RegistroCredencialRemoto(
        Integer idUsuario,
        String rut,
        String clavePlana,
        Boolean requiereCambioClave
) {}