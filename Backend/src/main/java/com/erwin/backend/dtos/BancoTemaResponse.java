package com.erwin.backend.dtos;

import java.time.LocalDate;

public record BancoTemaResponse(
        Integer idTema,
        String titulo,
        String descripcion,
        String carrera,
        String docente,
        String estado,
        String observaciones,
        Integer idEstudianteSugerente,
        String aprobadoPorConsejo,
        LocalDate fechaAprobacionConsejo
) {}
