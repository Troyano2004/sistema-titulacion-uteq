package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EjeTematicoRequest {
    private Integer idCarrera;
    private String nombre;
    private String descripcion;
    private Integer idDocenteResponsable;
    private String periodoAcademico;
    private String estado;
}
