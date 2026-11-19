package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfiguracionModalidadEntregaRequest {
    private Integer idDocente;
    private Integer idCarrera;
    private Integer idPeriodo;
    private String modalidadEntrega; // REDACCION_SISTEMA / UPLOAD_PDF
}
