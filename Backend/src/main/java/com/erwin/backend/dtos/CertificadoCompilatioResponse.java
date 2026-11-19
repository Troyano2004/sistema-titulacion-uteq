package com.erwin.backend.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CertificadoCompilatioResponse(
        Integer idIntento,
        Integer idProyecto,
        BigDecimal porcentaje,
        Boolean certificadoEmitido,
        LocalDateTime fechaEmision,
        String firmadoPor
) {}
