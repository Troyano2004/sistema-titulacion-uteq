package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoDocumentoTic;
import com.erwin.backend.enums.ModalidadEntrega;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Modelo flexible por secciones del documento del TIC (reemplaza en
 * paralelo a {@link DocumentoTitulacion}, que se mantiene intacto porque
 * Dt2Service y todo el pipeline de antiplagio/predefensa/sustentación
 * siguen dependiendo de él — ver REGLAS de la TAREA).
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documento_tic")
public class DocumentoTIC {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_tic")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tipo_trabajo", nullable = false)
    private Tipotrabajotitulacion tipoTrabajo;

    @ManyToOne
    @JoinColumn(name = "id_director")
    private Docente director;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_entrega", length = 20, nullable = false)
    private ModalidadEntrega modalidadEntrega;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoDocumentoTic estado = EstadoDocumentoTic.BORRADOR;

    @Column(name = "titulo", length = 300)
    private String titulo;

    @Column(name = "ciudad", length = 100)
    private String ciudad = "Quevedo";

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "fecha_ultima_modificacion", nullable = false)
    private LocalDateTime fechaUltimaModificacion = LocalDateTime.now();

    /** Para modalidad UPLOAD_PDF — URL en Azure Blob Storage (ver DocumentStorageService). */
    @Column(name = "archivo_pdf_url", length = 500)
    private String archivoPdfUrl;
}
