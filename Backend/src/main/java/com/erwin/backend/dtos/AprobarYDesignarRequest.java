package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AprobarYDesignarRequest {
    private Integer idUsuario;
    private Integer directorId;
    private Integer presidenteTribunalId;
    private Integer vocal1Id;
    private Integer vocal2Id;
    private String observaciones;
}
