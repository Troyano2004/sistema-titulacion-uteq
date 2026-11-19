package com.erwin.backend.dtos;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class BancoPreguntasDtos {

    public record OpcionRequest(
            Long id,
            String texto,
            Boolean esCorrecta,
            Integer orden
    ) {}

    public record PreguntaComplexivoRequest(
            Long idEjeTematico,
            Integer idCarrera,
            String enunciado,
            String dificultad,
            String estado,
            String periodoAcademico,
            Integer idDocenteCreador,
            List<OpcionRequest> opciones
    ) {}

    public record OpcionResponse(
            Long id,
            String texto,
            Boolean esCorrecta,
            Integer orden
    ) {}

    public record PreguntaComplexivoResponse(
            Long id,
            Long idEjeTematico,
            String nombreEjeTematico,
            Integer idCarrera,
            String enunciado,
            String dificultad,
            String estado,
            Integer idDocenteCreador,
            String nombreDocenteCreador,
            LocalDateTime fechaCreacion,
            String periodoAcademico,
            List<OpcionResponse> opciones
    ) {}

    public record EstadisticasBancoDto(
            Integer idCarrera,
            long totalActivas,
            Map<String, Long> porEjeTematico,
            long minimoRequerido,
            boolean cumpleMinimo
    ) {}
}
