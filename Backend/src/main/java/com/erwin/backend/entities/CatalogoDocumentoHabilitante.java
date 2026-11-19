package com.erwin.backend.entities;

import com.erwin.backend.enums.ModalidadCatalogoDocumento;
import com.erwin.backend.enums.ResponsableDocumento;
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
@Table(name = "catalogo_documento_habilitante")
public class CatalogoDocumentoHabilitante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_catalogo")
    private Long id;

    @Column(name = "codigo", length = 5, nullable = false)
    private String codigo;

    @Column(name = "nombre", length = 300, nullable = false)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", length = 15, nullable = false)
    private ModalidadCatalogoDocumento modalidad;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "obligatorio", nullable = false)
    private Boolean obligatorio = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "responsable", length = 20, nullable = false)
    private ResponsableDocumento responsable;

    @ManyToOne
    @JoinColumn(name = "id_facultad")
    private Facultad facultad;
}
