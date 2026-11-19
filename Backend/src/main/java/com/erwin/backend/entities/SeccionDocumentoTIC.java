package com.erwin.backend.entities;

import com.erwin.backend.enums.EstadoSeccionTic;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "seccion_documento_tic",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_documento_tic", "codigo_seccion"}))
public class SeccionDocumentoTIC {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seccion")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_documento_tic", nullable = false)
    private DocumentoTIC documento;

    @Column(name = "codigo_seccion", length = 50, nullable = false)
    private String codigoSeccion;

    @Column(name = "titulo", length = 150, nullable = false)
    private String titulo;

    @Column(name = "contenido_html", columnDefinition = "TEXT")
    private String contenidoHtml;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "obligatoria", nullable = false)
    private Boolean obligatoria = true;

    @Column(name = "auto_llenada", nullable = false)
    private Boolean autoLlenada = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 15, nullable = false)
    private EstadoSeccionTic estado = EstadoSeccionTic.VACIO;
}
