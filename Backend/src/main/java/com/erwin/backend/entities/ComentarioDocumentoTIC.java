package com.erwin.backend.entities;

import com.erwin.backend.enums.RolAutorComentarioTic;
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
@Table(name = "comentario_documento_tic")
public class ComentarioDocumentoTIC {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comentario")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_documento_tic", nullable = false)
    private DocumentoTIC documento;

    /** Nulo = comentario general (no ligado a una sección específica). */
    @ManyToOne
    @JoinColumn(name = "id_seccion")
    private SeccionDocumentoTIC seccion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario_autor", nullable = false)
    private Usuario autor;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_autor", length = 25, nullable = false)
    private RolAutorComentarioTic rolAutor;

    @Column(name = "contenido", columnDefinition = "TEXT", nullable = false)
    private String contenido;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "resuelto", nullable = false)
    private Boolean resuelto = false;
}
