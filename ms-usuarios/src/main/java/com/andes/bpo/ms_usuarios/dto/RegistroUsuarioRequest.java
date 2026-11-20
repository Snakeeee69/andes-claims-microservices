package com.andes.bpo.ms_usuarios.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record RegistroUsuarioRequest(
        @Schema(example = "198765432")
        @NotBlank(message = "El RUT es obligatorio") String rut,

        @Schema(example = "Diego")
        @NotBlank(message = "El nombre es obligatorio") String nombre,

        @Schema(example = "Valenzuela")
        @NotBlank(message = "El apellido es obligatorio") String apellido,

        @Schema(description = "Fecha de nacimiento en formato d/M/yyyy", type = "string", example = "15/08/1998")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "d/M/yyyy") LocalDate fechaNacimiento,

        @Schema(example = "diego.valenzuela@correo.cl")
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Formato de correo inválido") String correo,

        @Schema(example = "+56991234567")
        String telefono,

        @Schema(example = "Av. San Pablo 4210, Quinta Normal")
        String direccion,

        @Schema(description = "Compañía aseguradora del usuario", example = "BCI Seguros")
        String aseguradora,

        @Schema(example = "MiClave2026")
        String passwordHash,

        @Schema(example = "ASEGURADO_NATURAL")
        String rol,

        String equipo,
        String tipoLiquidador,
        Integer idArea
) {}