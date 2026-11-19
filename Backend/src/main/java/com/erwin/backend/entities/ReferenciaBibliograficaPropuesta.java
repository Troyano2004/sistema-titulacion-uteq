package com.erwin.backend.entities;

import com.erwin.backend.enums.TipoReferenciaBibliografica;
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
@Table(name = "referencia_bibliografica_propuesta")
public class ReferenciaBibliograficaPropuesta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_referencia")
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_propuesta", nullable = false)
    private PropuestaTitulacion propuesta;

    @Column(name = "autores", length = 500, nullable = false)
    private String autores;

    @Column(name = "titulo", length = 400, nullable = false)
    private String titulo;

    @Column(name = "anio_publicacion", nullable = false)
    private Integer anioPublicacion;

    @Column(name = "fuente", length = 400)
    private String fuente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", length = 15, nullable = false)
    private TipoReferenciaBibliografica tipo;
}
