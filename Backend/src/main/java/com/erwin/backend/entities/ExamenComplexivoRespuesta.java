package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "examen_complexivo_respuesta",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_aplicacion", "id_pregunta"}))
public class ExamenComplexivoRespuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_aplicacion", nullable = false)
    private ExamenComplexivoAplicacion examen;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pregunta", nullable = false)
    private PreguntaComplexivo pregunta;

    @ManyToOne
    @JoinColumn(name = "id_opcion_seleccionada")
    private OpcionPregunta opcionSeleccionada;

    @Column(name = "orden", nullable = false)
    private Integer orden;
}
