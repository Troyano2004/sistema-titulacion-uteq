package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoExpediente;
import com.erwin.backend.enums.ModalidadCatalogoDocumento;
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
@Table(name = "expediente_estudiante")
public class ExpedienteEstudiante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_expediente")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", length = 15, nullable = false)
    private ModalidadCatalogoDocumento modalidad;

    @Column(name = "periodo_titulacion", length = 20)
    private String periodoTitulacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoExpediente estado = EstadoExpediente.INCOMPLETO;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
