package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AprobarModificacionRequest {
    private Integer idUsuario;
    private String observaciones;
}
