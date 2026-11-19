package com.erwin.backend.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionModalidadEntregaResponse {
    private Integer idCarrera;
    private Integer idPeriodo;
    private String modalidadEntrega;
}
