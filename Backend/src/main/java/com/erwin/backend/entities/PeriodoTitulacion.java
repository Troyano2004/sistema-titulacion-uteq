package com.erwin.backend.entities;

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
@Table(name = "periodo_titulacion")
public class PeriodoTitulacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_periodo")
    private Integer idPeriodo;

    @Column(name = "descripcion", length = 100, nullable = false)
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "activo")
    private Boolean activo = true;

    // ── Ventanas de los cortes evaluativos de Titulación II (Módulo 8) ──────
    // Nullable: si no se configuran, las validaciones de plazo del corte se omiten.

    @Column(name = "fecha_inicio_corte1")
    private LocalDate fechaInicioCorte1;

    @Column(name = "fecha_fin_corte1")
    private LocalDate fechaFinCorte1;

    @Column(name = "fecha_inicio_corte2")
    private LocalDate fechaInicioCorte2;

    @Column(name = "fecha_fin_corte2")
    private LocalDate fechaFinCorte2;

    // ── Aval de Unidad de Registro (Módulo 18) ───────────────────────────
    @Column(name = "aval_unidad_registro")
    private Boolean avalUnidadRegistro = false;

    @Column(name = "fecha_aval")
    private LocalDate fechaAval;

    @Column(name = "avalado_por", length = 150)
    private String avaladoPor;
}
