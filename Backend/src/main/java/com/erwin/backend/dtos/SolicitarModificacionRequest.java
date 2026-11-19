package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SolicitarModificacionRequest {
    private Integer idDocenteSolicitante;
    private String motivoModificacion;
    private List<String> camposAModificar;
}
