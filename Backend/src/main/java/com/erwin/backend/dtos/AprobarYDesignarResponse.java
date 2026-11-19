package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AprobarYDesignarResponse {
    private Integer idPropuesta;
    private Integer idProyecto;
    private String director;
    private String presidenteTribunal;
    private String vocal1;
    private String vocal2;
    private String mensaje;
}
