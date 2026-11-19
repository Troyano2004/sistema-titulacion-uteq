package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoDocumentoExpediente;
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
@Table(name = "documento_expediente")
public class DocumentoExpediente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_expediente")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_expediente", nullable = false)
    private ExpedienteEstudiante expediente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_catalogo", nullable = false)
    private CatalogoDocumentoHabilitante catalogoDocumento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoDocumentoExpediente estado = EstadoDocumentoExpediente.PENDIENTE;

    /**
     * URL del archivo en Azure Blob Storage (ver DocumentStorageService).
     * La tarea original pedía {@code archivoId: Long}, pero el almacenamiento
     * real (igual que DocumentoTIC.archivoPdfUrl) devuelve una URL, no un id
     * numérico — se mantiene la misma convención que el resto del sistema.
     */
    @Column(name = "archivo_url", length = 500)
    private String archivoUrl;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "id_usuario_valida")
    private Usuario validadoPor;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    @Column(name = "motivo_rechazo", columnDefinition = "TEXT")
    private String motivoRechazo;
}
