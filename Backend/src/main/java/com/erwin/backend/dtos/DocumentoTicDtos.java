package com.erwin.backend.dtos;

import java.time.LocalDateTime;
import java.util.List;

public class DocumentoTicDtos {

    public record CrearDocumentoTicRequest(
            Integer idEstudiante,
            Integer idTipoTrabajo,
            Integer idDirector,
            String modalidadEntrega, // REDACCION_SISTEMA / UPLOAD_PDF
            String titulo
    ) {}

    public record SeccionTicResponse(
            Long id,
            String codigoSeccion,
            String titulo,
            String contenidoHtml,
            Integer orden,
            Boolean obligatoria,
            Boolean autoLlenada,
            String estado
    ) {}

    public record DocumentoTicResponse(
            Long id,
            Integer idEstudiante,
            String nombreEstudiante,
            Integer idTipoTrabajo,
            String nombreTipoTrabajo,
            Integer idDirector,
            String nombreDirector,
            String modalidadEntrega,
            String estado,
            String titulo,
            String ciudad,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaUltimaModificacion,
            String archivoPdfUrl,
            Integer seccionesCompletas,
            Integer seccionesTotales,
            List<SeccionTicResponse> secciones
    ) {}

    public record GuardarSeccionRequest(
            String contenidoHtml
    ) {}

    public record RegistrarArchivoPdfRequest(
            String archivoPdfUrl
    ) {}

    public record CrearSeccionPersonalizadaRequest(
            String codigoSeccion,
            String titulo,
            Boolean obligatoria
    ) {}

    public record VersionTicResponse(
            Long id,
            Integer numeroVersion,
            LocalDateTime fechaVersion,
            String creadaPor
    ) {}

    public record VersionTicDetalleResponse(
            Long id,
            Integer numeroVersion,
            LocalDateTime fechaVersion,
            String creadaPor,
            String contenidoSnapshot
    ) {}

    public record CrearVersionRequest(
            String creadaPor
    ) {}

    public record ComentarioTicRequest(
            Long idSeccion, // null = comentario general
            Integer idUsuarioAutor,
            String rolAutor, // DIRECTOR / PRESIDENTE_TRIBUNAL / VOCAL_TRIBUNAL
            String contenido
    ) {}

    public record ComentarioTicResponse(
            Long id,
            Long idSeccion,
            String codigoSeccion,
            Integer idAutor,
            String nombreAutor,
            String rolAutor,
            String contenido,
            LocalDateTime fechaCreacion,
            Boolean resuelto
    ) {}

    public record ReferenciaBibliograficaRequest(
            String autores,
            String titulo,
            Integer anioPublicacion,
            String fuente,
            String tipo
    ) {}

    public record ReferenciaBibliograficaResponse(
            Long id,
            String autores,
            String titulo,
            Integer anioPublicacion,
            String fuente,
            String tipo
    ) {}

    public record BibliografiaFormateadaResponse(
            String estandar,
            List<String> referenciasFormateadas,
            int total,
            int minimoRequerido,
            boolean cumpleMinimo,
            double porcentajeRecientes,
            double porcentajeMinimoRecientes,
            boolean cumpleRecientes
    ) {}
}
