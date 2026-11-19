package com.erwin.backend.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class CalendarioDtos {

    public record EventoCalendarioDto(
            String tipoEvento,
            LocalDate fecha,
            LocalTime hora,
            String lugar,
            String descripcion,
            Integer idCarrera,
            String nombreCarrera,
            List<String> estudiantes,
            List<String> docentes
    ) {}

    public record AvalUnidadRegistroRequest(
            String avaladoPor
    ) {}

    public record PeriodoAvalDto(
            Integer idPeriodo,
            String periodo,
            Boolean avalUnidadRegistro,
            LocalDate fechaAval,
            String avaladoPor
    ) {}
}
