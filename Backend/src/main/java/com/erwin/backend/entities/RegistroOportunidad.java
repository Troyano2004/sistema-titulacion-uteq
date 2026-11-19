package com.erwin.backend.entities;

import com.erwin.backend.enums.ModalidadOportunidad;
import com.erwin.backend.enums.ResultadoOportunidad;
import com.erwin.backend.enums.TipoOportunidad;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "registro_oportunidad")
public class RegistroOportunidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_periodo", nullable = false)
    private PeriodoTitulacion periodoTitulacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", length = 35, nullable = false)
    private ModalidadOportunidad modalidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_oportunidad", length = 15, nullable = false)
    private TipoOportunidad tipoOportunidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", length = 15, nullable = false)
    private ResultadoOportunidad resultado;

    @Column(name = "calificacion", precision = 4, scale = 2)
    private BigDecimal calificacion;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    // ── Tercera matrícula (TAREA 1.3.c) ──────────────────────────────────
    @Column(name = "autorizacion_tercera_matricula")
    private Boolean autorizacionTerceraMatricula = false;

    @Column(name = "resolucion_tercera_matricula", length = 200)
    private String resolucionTerceraMatricula;
}
