package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoJustificacion;
import com.erwin.backend.enums.TipoDocumentoJustificacion;
import com.erwin.backend.enums.TipoJustificacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "solicitud_justificacion")
public class SolicitudJustificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_solicitud")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 30, nullable = false)
    private TipoJustificacion tipo;

    @Column(name = "motivo", columnDefinition = "TEXT", nullable = false)
    private String motivo;

    @Column(name = "documento_adjunto_id")
    private Long documentoAdjuntoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", length = 30)
    private TipoDocumentoJustificacion tipoDocumento;

    // Fecha del evento que se justifica (examen/sustentación no rendida) —
    // usada para calcular fechaLimite según el tipo.
    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoJustificacion estado = EstadoJustificacion.PENDIENTE;

    @ManyToOne
    @JoinColumn(name = "id_usuario_resuelve")
    private Usuario resueltaPor;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "nueva_fecha_programada")
    private LocalDate nuevaFechaProgramada;

    @Column(name = "observaciones_resolucion", columnDefinition = "TEXT")
    private String observacionesResolucion;
}
