package com.erwin.backend.dtos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RegistrarResultadoAntiplagioRequest {
    private Integer estudianteId;
    private BigDecimal porcentaje;
    private String observaciones;
    private Long archivoReporteId;
}
