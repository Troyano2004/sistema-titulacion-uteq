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
@Table(name = "opcion_pregunta")
public class OpcionPregunta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_opcion")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_pregunta", nullable = false)
    private PreguntaComplexivo pregunta;

    @Column(name = "texto", columnDefinition = "TEXT", nullable = false)
    private String texto;

    @Column(name = "es_correcta", nullable = false)
    private Boolean esCorrecta = false;

    @Column(name = "orden", nullable = false)
    private Integer orden;
}
