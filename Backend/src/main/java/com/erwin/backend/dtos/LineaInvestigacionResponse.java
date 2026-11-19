package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LineaInvestigacionResponse {
    private Long id;
    private Integer idCarrera;
    private String nombreCarrera;
    private String nombre;
    private String descripcion;
    private String estado;
    private LocalDate fechaAprobacion;
}
