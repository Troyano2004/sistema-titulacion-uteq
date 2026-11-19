package com.erwin.backend.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class JustificacionDtos {

    public record SolicitarJustificacionRequest(
            Integer idEstudiante,
            String tipo,
            String motivo,
            Long documentoAdjuntoId,
            String tipoDocumento,
            LocalDateTime fechaEvento
    ) {}

    public record SolicitudJustificacionDto(
            Long id,
            Integer idEstudiante,
            String nombreEstudiante,
            String tipo,
            String motivo,
            Long documentoAdjuntoId,
            String tipoDocumento,
            LocalDateTime fechaEvento,
            LocalDateTime fechaSolicitud,
            LocalDateTime fechaLimite,
            String estado,
            Integer idUsuarioResuelve,
            String nombreResuelve,
            LocalDateTime fechaResolucion,
            LocalDate nuevaFechaProgramada,
            String observacionesResolucion
    ) {}

    public record ResolverJustificacionRequest(
            String estado, // APROBADA / RECHAZADA
            LocalDate nuevaFechaProgramada,
            String observacionesResolucion,
            Integer idUsuarioResuelve
    ) {}
}
