package com.erwin.backend.dtos;

import java.time.LocalDateTime;
import java.util.List;

public class ExpedienteDtos {

    public record CrearExpedienteRequest(
            Integer idEstudiante,
            String modalidad, // TIC / COMPLEXIVO
            String periodoTitulacion
    ) {}

    public record DocumentoExpedienteDto(
            Long id,
            String codigo,
            String nombre,
            String descripcion,
            String modalidadCatalogo,
            Integer orden,
            Boolean obligatorio,
            String responsable,
            String estado,
            String archivoUrl,
            String observaciones,
            String validadoPorNombre,
            LocalDateTime fechaValidacion,
            String motivoRechazo
    ) {}

    public record ExpedienteDto(
            Long id,
            Integer idEstudiante,
            String nombreEstudiante,
            String modalidad,
            String periodoTitulacion,
            String estado,
            LocalDateTime fechaCreacion,
            List<DocumentoExpedienteDto> documentos
    ) {}

    public record CargarDocumentoRequest(
            String archivoUrl,
            String observaciones
    ) {}

    public record ValidarDocumentoRequest(
            Integer idUsuarioValida
    ) {}

    public record RechazarDocumentoRequest(
            Integer idUsuarioValida,
            String motivoRechazo
    ) {}

    public record ProgresoExpedienteDto(
            Long idExpediente,
            int total,
            int pendientes,
            int cargados,
            int validados,
            int aprobados,
            int rechazados,
            int faltantesObligatorios
    ) {}

    public record AlertaExpedienteDto(
            Long idExpediente,
            Integer idEstudiante,
            String nombreEstudiante,
            Long idDocumentoExpediente,
            String codigo,
            String nombreDocumento,
            String responsable,
            java.time.LocalDate fechaEvento,
            long diasRestantes
    ) {}
}
