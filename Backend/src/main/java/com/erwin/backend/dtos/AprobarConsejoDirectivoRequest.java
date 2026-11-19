package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AprobarConsejoDirectivoRequest {
    private Integer idUsuario;
    private String observaciones;
}
