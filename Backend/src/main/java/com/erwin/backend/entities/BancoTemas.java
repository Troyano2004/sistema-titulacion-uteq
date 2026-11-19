package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoBancoTemas;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "banco_temas")
public class BancoTemas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tema")
    private Integer idTema;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_carrera")
    private Carrera carrera;

    @ManyToOne
    @JoinColumn(name = "id_docente_proponente")
    private Docente docenteProponente;

    // ✅ NUEVO: estudiante que sugirió el tema
    @ManyToOne
    @JoinColumn(name = "id_estudiante_sugerente")
    private Estudiante estudianteSugerente;

    @Column(name = "titulo", length = 500, nullable = false)
    private String titulo;

    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30)
    private EstadoBancoTemas estado;

    @Column(name = "fecha_revision")
    private LocalDate fechaRevision;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "id_usuario_aprobo_consejo")
    private Usuario aprobadoPorConsejo;

    @Column(name = "fecha_aprobacion_consejo")
    private LocalDate fechaAprobacionConsejo;
}