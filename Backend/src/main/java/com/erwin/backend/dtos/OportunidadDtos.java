package com.erwin.backend.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OportunidadDtos {

    public record RegistroOportunidadDto(
            Long id,
            Integer idEstudiante,
            String nombreEstudiante,
            Integer idPeriodo,
            String periodo,
            String modalidad,
            String tipoOportunidad,
            String resultado,
            BigDecimal calificacion,
            LocalDate fecha,
            String observaciones
    ) {}

    public record ElegibilidadDto(
            boolean puede,
            String motivo,
            boolean requiereActualizacionConocimientos,
            boolean requiereAutorizacionTerceraMatricula,
            int oportunidadesUsadasEnPeriodo,
            int periodosDeTitulacionUsados
    ) {}

    public record SolicitarActualizacionRequest(
            Integer idEstudiante,
            String resolucionConsejoDirectivo,
            List<String> asignaturasAdicionales,
            String periodoOriginal,
            String periodoActualizacion
    ) {}

    public record ActualizacionConocimientosDto(
            Long id,
            Integer idEstudiante,
            String nombreEstudiante,
            LocalDateTime fechaSolicitud,
            String estado,
            String resolucionConsejoDirectivo,
            LocalDate fechaAprobacion,
            List<String> asignaturasAdicionales,
            String periodoOriginal,
            String periodoActualizacion
    ) {}

    public record AprobarActualizacionRequest(
            String estado, // APROBADA / RECHAZADA
            String resolucionConsejoDirectivo
    ) {}

    public record AutorizarTerceraMatriculaRequest(
            Integer idEstudiante,
            String resolucion,
            Integer idUsuarioAutoriza
    ) {}
}
