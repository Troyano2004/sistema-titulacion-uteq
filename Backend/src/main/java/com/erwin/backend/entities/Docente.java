package com.erwin.backend.entities;

import com.erwin.backend.enums.NivelPosgrado;
import com.erwin.backend.enums.TipoVinculacion;
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
@Table(name = "docente")
public class Docente {
    @Id
    @Column(name = "id_docente")
    private Integer idDocente;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "id_docente")
    private Usuario usuario;

    /** @deprecated reemplazado por {@link #tipoVinculacion}, {@link #nivelPosgrado} y {@link #areaConocimiento}; se mantiene para no perder datos existentes. */
    @Deprecated
    @Column(name = "titulo_4to_nivel", length = 200)
    private String titulo4toNivel;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vinculacion", length = 20)
    private TipoVinculacion tipoVinculacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_posgrado", length = 20)
    private NivelPosgrado nivelPosgrado;

    @Column(name = "area_conocimiento", length = 200)
    private String areaConocimiento;
}