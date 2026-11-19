package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Dt1AsignarTutorRequest {
    private Integer idUsuario;
    private Integer idEstudiante;
    private Integer idDocente;
    private Integer idPeriodo;

    // Control de plazo (semana 20) para registrar la orientación DT1
    private Integer semanaActual;
}