package com.erwin.backend.entities;


import com.erwin.backend.enums.EstadoAnteproyecto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "anteproyecto_titulacion")
public class AnteproyectoTitulacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_anteproyecto")
    private Integer idAnteproyecto;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_propuesta", nullable = false, unique = true)
    private PropuestaTitulacion propuesta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_eleccion", nullable = false)
    private EleccionTitulacion eleccion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_carrera", nullable = false)
    private Carrera carrera;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 30, nullable = false)
    private EstadoAnteproyecto estado;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDate fechaCreacion;

    @Column(name = "fecha_ultima_actualizacion", insertable = false, updatable = false)
    private LocalDateTime fechaUltimaActualizacion;

    @OneToMany(mappedBy = "anteproyecto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Anteproyectotitulacionversion> versiones;

    // ── Flujo de modificaciones post-aprobación (excepción al bloqueo) ────────

    @Column(name = "motivo_modificacion_solicitada", columnDefinition = "TEXT")
    private String motivoModificacionSolicitada;

    @Column(name = "campos_a_modificar_solicitados", columnDefinition = "TEXT")
    private String camposAModificarSolicitados;

    @ManyToOne
    @JoinColumn(name = "id_docente_solicito_modificacion")
    private Docente solicitadoPor;
}