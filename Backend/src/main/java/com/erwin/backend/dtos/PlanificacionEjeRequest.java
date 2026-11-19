package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanificacionEjeRequest {
    private String cronograma;
    private String contenidosTematicos;
}
