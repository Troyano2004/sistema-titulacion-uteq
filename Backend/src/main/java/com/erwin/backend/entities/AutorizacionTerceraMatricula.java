package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Autorización especial para que un estudiante intente un tercer periodo de
 * titulación (TAREA 1.3.c). Entidad separada de {@link RegistroOportunidad}
 * porque la autorización debe existir ANTES de que se registre el primer
 * intento en ese tercer periodo — no hay todavía una fila de oportunidad a
 * la cual adjuntarla.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "autorizacion_tercera_matricula",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_estudiante", "id_periodo"}))
public class AutorizacionTerceraMatricula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_autorizacion")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_periodo", nullable = false)
    private PeriodoTitulacion periodoTitulacion;

    @Column(name = "resolucion", length = 200, nullable = false)
    private String resolucion;

    @Column(name = "fecha_autorizacion", nullable = false)
    private LocalDate fechaAutorizacion = LocalDate.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario_autoriza", nullable = false)
    private Usuario autorizadoPor;
}
