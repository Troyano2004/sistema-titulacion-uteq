package com.erwin.backend.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ExamenComplexivoDtos {

    public record ProgramarExamenRequest(
            Integer idEstudiante,
            String tipo,
            LocalDate fechaProgramada
    ) {}

    public record ExamenAplicacionResponse(
            Long id,
            Integer idEstudiante,
            String nombreEstudiante,
            Integer idCarrera,
            String nombreCarrera,
            Integer idPeriodo,
            String tipo,
            LocalDate fechaProgramada,
            LocalDateTime horaInicio,
            LocalDateTime horaFin,
            Integer duracionMinutos,
            String estado,
            BigDecimal calificacion,
            Integer totalPreguntasCorrectas,
            Integer idDelegado,
            String nombreDelegado,
            String observaciones
    ) {}

    public record RegistrarIngresoRequest(
            String documentoIdentidad,
            Boolean firmadoCompromisosEticos
    ) {}

    public record OpcionExamenDto(
            Long idOpcion,
            String texto,
            Integer orden
    ) {}

    public record PreguntaExamenDto(
            Long idPregunta,
            String enunciado,
            List<OpcionExamenDto> opciones
    ) {}

    public record IniciarExamenResponse(
            Long idExamen,
            LocalDateTime horaInicio,
            LocalDateTime horaFin,
            Integer duracionMinutos,
            List<PreguntaExamenDto> preguntas,
            Map<Long, Long> respuestasSeleccionadas
    ) {}

    public record ResponderPreguntaRequest(
            Long preguntaId,
            Long opcionSeleccionadaId
    ) {}

    public record ResultadoExamenDto(
            Long idExamen,
            BigDecimal calificacion,
            Integer totalPreguntasCorrectas,
            Integer totalPreguntas,
            boolean aprobado
    ) {}

    public record DelegadoDisponibleDto(
            Integer idDocente,
            String nombreDocente,
            Integer idCarrera,
            String nombreCarrera
    ) {}

    public record AsignarDelegadoRequest(
            Integer idDocente
    ) {}

    public record RegistrarFraudeRequest(
            String descripcionIncidente,
            String evidencia
    ) {}
}
