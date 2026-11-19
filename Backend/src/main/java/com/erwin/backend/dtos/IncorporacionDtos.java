package com.erwin.backend.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public class IncorporacionDtos {

    public record ActaGradoIncorporacionResponse(
            Integer idActa,
            Integer idEstudiante,
            String nombreEstudiante,
            String tipoTitulacion,
            String modalidad,
            BigDecimal promedioRecordAcademico,
            BigDecimal notaTitulacion,
            BigDecimal notaFinalGrado,
            String estado,
            Boolean firmaRector,
            Boolean firmaDecano,
            Boolean firmaSecretarioGeneral,
            LocalDate fechaCeremonia,
            LocalDate fechaEmision
    ) {}

    public record FirmarActaRequest(
            String tipoFirma // RECTOR / DECANO / SECRETARIO_GENERAL
    ) {}

    public record ProgramarCeremoniaRequest(
            LocalDate fechaCeremonia
    ) {}

    public record TituloProfesionalResponse(
            Long id,
            Integer idEstudiante,
            String nombreEstudiante,
            Integer idActa,
            String numeroTitulo,
            LocalDate fechaEmision,
            String registroSenescyt,
            LocalDate fechaRegistroSenescyt,
            String estado
    ) {}

    public record EmitirTituloRequest(
            String numeroTitulo
    ) {}

    public record RegistrarSenescytRequest(
            String registroSenescyt
    ) {}

    public record PendienteIncorporacionDto(
            Integer idActa,
            Integer idEstudiante,
            String nombreEstudiante,
            Integer idCarrera,
            String nombreCarrera,
            Integer idFacultad,
            String estado,
            Boolean firmaRector,
            Boolean firmaDecano,
            Boolean firmaSecretarioGeneral
    ) {}
}
