package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MatriculaTitulacionResponse {
    private Long id;
    private Integer idEstudiante;
    private String nombreEstudiante;
    private String periodoAcademico;
    private String nivel;
    private Integer idOpcionTitulacion;
    private String nombreOpcionTitulacion;
    private Integer idSubtipoTrabajo;
    private String nombreSubtipoTrabajo;
    private LocalDateTime fechaMatricula;
    private String estado;
    private Integer numeroCambiosOpcion;
    private String motivoUltimoCambio;
    private LocalDateTime fechaUltimoCambio;
    private Long documentoAdjuntoIdCasoMedico;
}
