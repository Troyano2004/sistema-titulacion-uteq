package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoExamenComplexivo;
import com.erwin.backend.enums.TipoExamenComplexivo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "examen_complexivo_aplicacion")
public class ExamenComplexivoAplicacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aplicacion")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_periodo", nullable = false)
    private PeriodoTitulacion periodoTitulacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 15, nullable = false)
    private TipoExamenComplexivo tipo;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDate fechaProgramada;

    @Column(name = "hora_inicio")
    private LocalDateTime horaInicio;

    @Column(name = "hora_fin")
    private LocalDateTime horaFin;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos = 180;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoExamenComplexivo estado = EstadoExamenComplexivo.PROGRAMADO;

    @Column(name = "calificacion", precision = 4, scale = 2)
    private BigDecimal calificacion;

    @Column(name = "total_preguntas_correctas")
    private Integer totalPreguntasCorrectas;

    @ManyToOne
    @JoinColumn(name = "id_docente_delegado")
    private Docente delegadoSupervisor;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    // ── Acta de registro de ingreso (TAREA 3.5) ──────────────────────────
    @Column(name = "documento_identidad", length = 30)
    private String documentoIdentidad;

    @Column(name = "firmado_compromisos_eticos")
    private Boolean firmadoCompromisosEticos = false;

    @Column(name = "hora_ingreso")
    private LocalDateTime horaIngreso;
}
