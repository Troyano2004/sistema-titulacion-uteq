package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoEjeTematico;
import com.erwin.backend.enums.EstadoPlanificacionEje;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "eje_tematico")
public class EjeTematico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_eje_tematico")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "id_docente_responsable")
    private Docente docenteResponsable;

    @Column(name = "periodo_academico", length = 20, nullable = false)
    private String periodoAcademico;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoEjeTematico estado = EstadoEjeTematico.ACTIVO;

    // ── Planificación (Módulo 12) ────────────────────────────────────────
    @Column(name = "cronograma", columnDefinition = "TEXT")
    private String cronograma; // JSON con semanas y contenidos

    @Column(name = "contenidos_tematicos", columnDefinition = "TEXT")
    private String contenidosTematicos;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_planificacion", length = 30)
    private EstadoPlanificacionEje estadoPlanificacion;

    @Column(name = "fecha_planificacion")
    private LocalDateTime fechaPlanificacion;

    @Column(name = "fecha_aprobacion_planificacion")
    private LocalDateTime fechaAprobacionPlanificacion;

    @ManyToOne
    @JoinColumn(name = "id_usuario_aprobador_planificacion")
    private Usuario aprobadoPorPlanificacion;
}
