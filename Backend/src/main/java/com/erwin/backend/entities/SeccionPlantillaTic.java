package com.erwin.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Define, por tipo de plantilla de TIC (Art. 42/43/44/45 + preliminares del
 * Art. 59), qué secciones debe tener el documento y en qué orden. Reemplaza
 * el switch hardcodeado que antes vivía en SeccionPlantillaProvider — así
 * la estructura queda en la BD y se puede corregir/ajustar sin tocar
 * código (ver SeccionPlantillaTicService).
 */
@Getter
@Setter
@Entity
@Table(name = "seccion_plantilla_tic")
public class SeccionPlantillaTic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seccion_plantilla")
    private Integer id;

    /** Nombre de {@link com.erwin.backend.enums.TipoTicPlantilla} (PROYECTO_INVESTIGACION, etc.). */
    @Column(name = "tipo_plantilla", length = 30, nullable = false)
    private String tipoPlantilla;

    @Column(name = "codigo_seccion", length = 50, nullable = false)
    private String codigoSeccion;

    @Column(name = "titulo", length = 150, nullable = false)
    private String titulo;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "obligatoria", nullable = false)
    private Boolean obligatoria = true;

    @Column(name = "auto_llenada", nullable = false)
    private Boolean autoLlenada = false;
}
