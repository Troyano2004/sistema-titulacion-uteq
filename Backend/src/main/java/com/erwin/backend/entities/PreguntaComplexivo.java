package com.erwin.backend.entities;

import com.erwin.backend.enums.DificultadPregunta;
import com.erwin.backend.enums.EstadoPregunta;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pregunta_complexivo")
public class PreguntaComplexivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pregunta")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_eje_tematico", nullable = false)
    private EjeTematico ejeTematico;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    @Column(name = "enunciado", columnDefinition = "TEXT", nullable = false)
    private String enunciado;

    @OneToMany(mappedBy = "pregunta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OpcionPregunta> opciones = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "dificultad", length = 15, nullable = false)
    private DificultadPregunta dificultad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoPregunta estado = EstadoPregunta.EN_REVISION;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_docente_creador", nullable = false)
    private Docente creadaPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "periodo_academico", length = 20, nullable = false)
    private String periodoAcademico;
}
