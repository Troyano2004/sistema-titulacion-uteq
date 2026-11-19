package com.erwin.backend.entities;

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
@Table(name = "version_documento_tic")
public class VersionDocumentoTIC {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_version")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_documento_tic", nullable = false)
    private DocumentoTIC documento;

    @Column(name = "numero_version", nullable = false)
    private Integer numeroVersion;

    @Column(name = "fecha_version", nullable = false)
    private LocalDateTime fechaVersion = LocalDateTime.now();

    @Column(name = "creada_por", length = 200, nullable = false)
    private String creadaPor;

    @Column(name = "contenido_snapshot", columnDefinition = "TEXT", nullable = false)
    private String contenidoSnapshot;
}
