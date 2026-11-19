package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "carrera")
public class Carrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrera")
    private Integer idCarrera;

    @ManyToOne
    @JoinColumn(name = "id_facultad")
    private Facultad facultad;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    /**
     * Número total de niveles/semestres de la malla curricular. Se usa para
     * derivar el antepenúltimo nivel (orientación DT1) y el penúltimo nivel
     * (matrícula de Titulación I) exigidos por el reglamento UIC-UTEQ.
     * Nullable: debe configurarlo el Coordinador/Admin vía el catálogo de carreras.
     */
    @Column(name = "duracion_niveles")
    private Integer duracionNiveles;
}
