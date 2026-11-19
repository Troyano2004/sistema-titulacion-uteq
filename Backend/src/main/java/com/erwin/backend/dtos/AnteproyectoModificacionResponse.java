package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnteproyectoModificacionResponse {
    private Integer idAnteproyecto;
    private String estado;
    private String motivoModificacionSolicitada;
    private String camposAModificarSolicitados;
    private String mensaje;
}
