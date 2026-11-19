package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EjeTematicoResponse {
    private Long id;
    private Integer idCarrera;
    private String nombreCarrera;
    private String nombre;
    private String descripcion;
    private Integer idDocenteResponsable;
    private String nombreDocenteResponsable;
    private String periodoAcademico;
    private String estado;
    private String cronograma;
    private String contenidosTematicos;
    private String estadoPlanificacion;
    private java.time.LocalDateTime fechaPlanificacion;
    private java.time.LocalDateTime fechaAprobacionPlanificacion;
    private String aprobadoPorPlanificacion;
}
