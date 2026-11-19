package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "dt1_tutor_estudiante",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_estudiante", "id_periodo"})
)
public class Dt1TutorEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tutor_estudiante")
    private Integer idTutorEstudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_docente", nullable = false)
    private Docente docente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_periodo", nullable = false)
    private PeriodoTitulacion periodo;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Plazo (semana 20 del periodo) para registrar la orientación DT1.
     * Se calcula automáticamente al asignar el tutor: fecha de inicio del
     * periodo + 20 semanas.
     */
    @Column(name = "fecha_limite_orientacion")
    private LocalDate fechaLimiteOrientacion;
}