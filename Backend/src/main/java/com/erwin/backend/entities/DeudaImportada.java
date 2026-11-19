package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoDeudaImportada;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Módulo 20 — deudas administrativas importadas del SGA. */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "deuda_importada")
public class DeudaImportada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_deuda_importada")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @Column(name = "tipo_deuda", length = 100, nullable = false)
    private String tipoDeuda;

    @Column(name = "monto", precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoDeudaImportada estado;

    @Column(name = "fecha_importacion", nullable = false)
    private LocalDateTime fechaImportacion = LocalDateTime.now();
}
