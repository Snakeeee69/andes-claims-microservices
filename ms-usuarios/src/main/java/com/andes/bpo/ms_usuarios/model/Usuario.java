package com.andes.bpo.ms_usuarios.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idUsuario;

    @Column(nullable = false, unique = true, length = 12)
    private String rut;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String apellido;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "d/M/yyyy")
    private LocalDate fechaNacimiento;

    @Column(nullable = false, unique = true, length = 120)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(length = 200)
    private String direccion;

    @Column(length = 80)
    private String aseguradora;

    @Column(nullable = false, length = 40)
    private String rol;

    private String equipo;
    private String tipoLiquidador;
    private Integer idArea;
}