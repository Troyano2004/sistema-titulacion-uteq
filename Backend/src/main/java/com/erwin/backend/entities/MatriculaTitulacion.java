package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoMatricula;
import com.erwin.backend.enums.NivelMatricula;
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
@Table(name = "matricula_titulacion")
public class MatriculaTitulacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_matricula")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Column(name = "periodo_academico", length = 20, nullable = false)
    private String periodoAcademico;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", length = 20, nullable = false)
    private NivelMatricula nivel;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_opcion_titulacion", nullable = false)
    private Modalidadtitulacion opcionTitulacion;

    @ManyToOne
    @JoinColumn(name = "id_subtipo_trabajo")
    private Tipotrabajotitulacion subtipoTrabajo;

    @Column(name = "fecha_matricula", nullable = false)
    private LocalDateTime fechaMatricula;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20, nullable = false)
    private EstadoMatricula estado = EstadoMatricula.ACTIVA;

    @Column(name = "numero_cambios_opcion", nullable = false)
    private Integer numeroCambiosOpcion = 0;

    @Column(name = "motivo_ultimo_cambio", columnDefinition = "TEXT")
    private String motivoUltimoCambio;

    @Column(name = "fecha_ultimo_cambio")
    private LocalDateTime fechaUltimoCambio;

    /**
     * Referencia (id) al archivo de soporte del caso médico cuando el cambio
     * de opción se realiza fuera de la semana 4 del periodo. No hay todavía
     * una entidad genérica de "documento subido" en el sistema: se guarda
     * solo el id devuelto por el almacenamiento de archivos, sin FK.
     */
    @Column(name = "documento_adjunto_id_caso_medico")
    private Long documentoAdjuntoIdCasoMedico;
}
