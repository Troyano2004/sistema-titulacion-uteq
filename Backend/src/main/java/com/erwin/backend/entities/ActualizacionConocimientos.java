package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoActualizacionConocimientos;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "actualizacion_conocimientos")
public class ActualizacionConocimientos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_actualizacion")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoActualizacionConocimientos estado = EstadoActualizacionConocimientos.SOLICITADA;

    @Column(name = "resolucion_consejo_directivo", length = 200)
    private String resolucionConsejoDirectivo;

    @Column(name = "fecha_aprobacion")
    private LocalDate fechaAprobacion;

    @ElementCollection
    @CollectionTable(name = "actualizacion_conocimientos_asignatura",
            joinColumns = @JoinColumn(name = "id_actualizacion"))
    @Column(name = "asignatura", length = 150)
    private List<String> asignaturasAdicionales = new ArrayList<>();

    @Column(name = "periodo_original", length = 20, nullable = false)
    private String periodoOriginal;

    @Column(name = "periodo_actualizacion", length = 20)
    private String periodoActualizacion;
}
