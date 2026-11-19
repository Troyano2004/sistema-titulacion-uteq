package com.erwin.backend.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AntiplagioResultadoResponse(
        Integer idIntento,
        Integer idProyecto,
        BigDecimal porcentaje,
        Boolean favorable,
        String mensaje,
        LocalDateTime fecha
) {}
