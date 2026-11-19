package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LineaInvestigacionRequest {
    private Integer idCarrera;
    private String nombre;
    private String descripcion;
    private String estado;
    private LocalDate fechaAprobacion;
}
