package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoAsignaturaImportada;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Módulo 20 — puente temporal de importación manual del SGA (TAREA 2.2). */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pensum_importado")
public class PensumImportado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pensum_importado")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Column(name = "codigo_asignatura", length = 30, nullable = false)
    private String codigoAsignatura;

    @Column(name = "nombre_asignatura", length = 200, nullable = false)
    private String nombreAsignatura;

    @Column(name = "calificacion", precision = 4, scale = 2)
    private BigDecimal calificacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoAsignaturaImportada estado;

    @Column(name = "periodo_academico", length = 20)
    private String periodoAcademico;

    @Column(name = "fecha_importacion", nullable = false)
    private LocalDateTime fechaImportacion = LocalDateTime.now();
}
