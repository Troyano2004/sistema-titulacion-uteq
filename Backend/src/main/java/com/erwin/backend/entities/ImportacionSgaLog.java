package com.erwin.backend.entities;

import com.erwin.backend.enums.TipoImportacionSga;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Módulo 20 — historial de importaciones del SGA (TAREA 2.4). */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "importacion_sga_log")
public class ImportacionSgaLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 15, nullable = false)
    private TipoImportacionSga tipo;

    @Column(name = "nombre_archivo", length = 300)
    private String nombreArchivo;

    @Column(name = "total_filas", nullable = false)
    private Integer totalFilas = 0;

    @Column(name = "filas_ok", nullable = false)
    private Integer filasOk = 0;

    @Column(name = "filas_error", nullable = false)
    private Integer filasError = 0;

    @Column(name = "errores", columnDefinition = "TEXT")
    private String errores;

    @ManyToOne
    @JoinColumn(name = "id_usuario_importa")
    private Usuario importadoPor;

    @Column(name = "fecha_importacion", nullable = false)
    private LocalDateTime fechaImportacion = LocalDateTime.now();
}
