package com.erwin.backend.dtos;

public record VerificacionMensualResponse(
        Integer estudianteId,
        Integer mes,
        Integer anio,
        long totalReuniones,
        boolean cumplioMinimo
) {}
