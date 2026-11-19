package com.erwin.backend.dtos;

import java.time.LocalDateTime;

public record PropuestaPendienteResponse(
        Integer idPropuesta,
        String titulo,
        String estudiante,
        Integer idCarrera,
        String carrera,
        LocalDateTime fechaRecepcionComision,
        long diasHabilesTranscurridos
) {}
