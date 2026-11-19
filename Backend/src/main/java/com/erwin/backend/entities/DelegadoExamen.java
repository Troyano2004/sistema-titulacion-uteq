package com.erwin.backend.entities;

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
@Table(name = "delegado_examen")
public class DelegadoExamen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_delegado_examen")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_aplicacion", nullable = false)
    private ExamenComplexivoAplicacion examen;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_docente", nullable = false)
    private Docente docente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carreraDocente;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();
}
