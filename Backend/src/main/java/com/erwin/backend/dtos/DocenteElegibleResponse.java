package com.erwin.backend.dtos;

public record DocenteElegibleResponse(
        Integer idDocente,
        String nombre,
        String tipoVinculacion,
        String nivelPosgrado
) {}
