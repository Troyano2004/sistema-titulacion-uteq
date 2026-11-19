package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MatriculaTitulacionRequest {
    private Integer idEstudiante;
    private String periodoAcademico;
    private String nivel;
    private Integer idOpcionTitulacion;
    private Integer idSubtipoTrabajo;

    // Art. 18 reglamento UIC-UTEQ — solo se validan para nivel TITULACION_II
    private Boolean idiomaExtranjeroAprobado;
    private Boolean practicasLaboralesAprobadas;
    private Boolean servicioComunitarioAprobado;
}
