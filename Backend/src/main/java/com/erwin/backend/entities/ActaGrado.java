package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoActaGrado;
import com.erwin.backend.enums.ModalidadActaGrado;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Módulo 19 — el cálculo 80/20 y el registro base ya lo escriben
 * Dt2Service.consolidarResultado (TIC) y ComplexivoService.calcularNotaGrado
 * (Complexivo) en notaRecord/notaTitulacion/notaFinal — esos campos y ese
 * flujo NO se tocan. Los campos nuevos (promedioRecordAcademico,
 * notaFinalGrado, modalidad, estado, firmas) son la capa de incorporación
 * que ActaGradoService.generar() agrega ENCIMA de un acta ya calculada,
 * reutilizando la misma fórmula (ver REGLAS).
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "acta_grado")
public class ActaGrado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_acta")
    private Integer idActa;

    @OneToOne
    @JoinColumn(name = "id_estudiante", unique = true)
    private Estudiante estudiante;

    @Column(name = "tipo_titulacion", length = 40)
    private String tipoTitulacion; // TRABAJO_INTEGRACION_CURRICULAR / EXAMEN_COMPLEXIVO

    @Column(name = "id_origen", nullable = false)
    private Integer idOrigen; // depende del tipo: id_proyecto o id_complexivo (no hay FK directa)

    @Column(name = "nota_record", precision = 5, scale = 2)
    private BigDecimal notaRecord;

    @Column(name = "nota_titulacion", precision = 5, scale = 2)
    private BigDecimal notaTitulacion;

    @Column(name = "nota_final", precision = 5, scale = 2)
    private BigDecimal notaFinal;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision = LocalDate.now();

    // ── Incorporación (Módulo 19, TAREA 1.1) ─────────────────────────────
    @Column(name = "promedio_record_academico", precision = 5, scale = 2)
    private BigDecimal promedioRecordAcademico;

    @Column(name = "nota_final_grado", precision = 5, scale = 2)
    private BigDecimal notaFinalGrado;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", length = 20)
    private ModalidadActaGrado modalidad;

    @Column(name = "fecha_ceremonia")
    private LocalDate fechaCeremonia;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 25)
    private EstadoActaGrado estado;

    @Column(name = "firma_rector", nullable = false)
    private Boolean firmaRector = false;

    @Column(name = "firma_decano", nullable = false)
    private Boolean firmaDecano = false;

    @Column(name = "firma_secretario_general", nullable = false)
    private Boolean firmaSecretarioGeneral = false;
}
