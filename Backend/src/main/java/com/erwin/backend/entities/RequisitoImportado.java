package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Módulo 20 — requisitos de Art. 18 importados del SGA (idioma/prácticas/servicio comunitario). */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "requisito_importado")
public class RequisitoImportado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_requisito_importado")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false, unique = true)
    private Estudiante estudiante;

    @Column(name = "idioma_aprobado", nullable = false)
    private Boolean idiomaAprobado = false;

    @Column(name = "practicas_laborales_aprobadas", nullable = false)
    private Boolean practicasLaborales = false;

    @Column(name = "horas_practicas")
    private Integer horasPracticas;

    @Column(name = "servicio_comunitario_aprobado", nullable = false)
    private Boolean servicioComunitario = false;

    @Column(name = "horas_servicio")
    private Integer horasServicio;

    @Column(name = "fecha_importacion", nullable = false)
    private LocalDateTime fechaImportacion = LocalDateTime.now();
}
