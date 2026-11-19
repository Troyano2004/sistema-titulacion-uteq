package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambiarOpcionMatriculaRequest {
    private Integer idOpcionTitulacion;
    private Integer idSubtipoTrabajo;
    private String motivo;

    // Control de plazo (semana 4). Obligatorio.
    private Integer semanaActual;

    // Requeridos solo cuando semanaActual > 4 (excepción por caso médico)
    private String motivoMedico;
    private Long documentoAdjuntoId;
}
