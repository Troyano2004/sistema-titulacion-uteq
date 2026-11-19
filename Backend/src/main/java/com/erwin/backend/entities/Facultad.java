package com.erwin.backend.entities;

import com.erwin.backend.enums.EstandarBibliografico;
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
@Table(name = "facultad")
public class Facultad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_facultad")
    private Integer idFacultad;

    @ManyToOne
    @JoinColumn(name = "id_universidad")
    private Universidad universidad;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estandar_bibliografico", length = 20)
    private EstandarBibliografico estandarBibliografico;

    /** Docente designado como decano de esta facultad (opcional). */
    @ManyToOne
    @JoinColumn(name = "id_docente_decano")
    private Docente decano;
}
