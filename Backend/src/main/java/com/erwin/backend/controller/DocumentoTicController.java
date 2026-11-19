package com.erwin.backend.controller;

import com.erwin.backend.dtos.DocumentoTicDtos.*;
import com.erwin.backend.service.BibliografiaService;
import com.erwin.backend.service.DocumentoTicService;
import com.erwin.backend.service.ExportacionDocumentoTicService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documentos-tic")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','DOCENTE','DOCENTE_TITULADO','COORDINADOR')")
public class DocumentoTicController {

    private final DocumentoTicService service;
    private final BibliografiaService bibliografiaService;
    private final ExportacionDocumentoTicService exportacionService;

    public DocumentoTicController(DocumentoTicService service,
                                   BibliografiaService bibliografiaService,
                                   ExportacionDocumentoTicService exportacionService) {
        this.service = service;
        this.bibliografiaService = bibliografiaService;
        this.exportacionService = exportacionService;
    }

    // ── Documento ────────────────────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','COORDINADOR')")
    @PostMapping
    public ResponseEntity<DocumentoTicResponse> crear(@RequestBody CrearDocumentoTicRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoTicResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','COORDINADOR','DOCENTE','DOCENTE_TITULADO')")
    @GetMapping("/estudiante/{idEstudiante}")
    public ResponseEntity<DocumentoTicResponse> obtenerPorEstudiante(@PathVariable Integer idEstudiante) {
        return service.obtenerPorEstudiante(idEstudiante)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ── Secciones ────────────────────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @PutMapping("/{id}/secciones/{codigoSeccion}")
    public ResponseEntity<SeccionTicResponse> guardarSeccion(
            @PathVariable Long id, @PathVariable String codigoSeccion, @RequestBody GuardarSeccionRequest req) {
        return ResponseEntity.ok(service.guardarSeccion(id, codigoSeccion, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/{id}/secciones")
    public ResponseEntity<SeccionTicResponse> crearSeccionPersonalizada(
            @PathVariable Long id, @RequestBody CrearSeccionPersonalizadaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearSeccionPersonalizada(id, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @DeleteMapping("/{id}/secciones/{codigoSeccion}")
    public ResponseEntity<Void> eliminarSeccionPersonalizada(@PathVariable Long id, @PathVariable String codigoSeccion) {
        service.eliminarSeccionPersonalizada(id, codigoSeccion);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @PutMapping("/{id}/archivo-pdf")
    public ResponseEntity<DocumentoTicResponse> registrarArchivoPdf(
            @PathVariable Long id, @RequestBody RegistrarArchivoPdfRequest req) {
        return ResponseEntity.ok(service.registrarArchivoPdf(id, req));
    }

    // ── Versiones ────────────────────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/{id}/versiones")
    public ResponseEntity<VersionTicResponse> crearVersion(@PathVariable Long id, @RequestBody CrearVersionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearVersion(id, req));
    }

    @GetMapping("/{id}/versiones")
    public ResponseEntity<List<VersionTicResponse>> listarVersiones(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarVersiones(id));
    }

    @GetMapping("/{id}/versiones/{versionId}")
    public ResponseEntity<VersionTicDetalleResponse> obtenerVersion(@PathVariable Long id, @PathVariable Long versionId) {
        return ResponseEntity.ok(service.obtenerVersion(id, versionId));
    }

    // ── Comentarios (director/tribunal — visibles para el estudiante) ──
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/{id}/comentarios")
    public ResponseEntity<ComentarioTicResponse> agregarComentario(@PathVariable Long id, @RequestBody ComentarioTicRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agregarComentario(id, req));
    }

    @GetMapping("/{id}/comentarios")
    public ResponseEntity<List<ComentarioTicResponse>> listarComentarios(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarComentarios(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','DOCENTE','DOCENTE_TITULADO')")
    @PutMapping("/{id}/comentarios/{comentarioId}/resolver")
    public ResponseEntity<ComentarioTicResponse> resolverComentario(@PathVariable Long id, @PathVariable Long comentarioId) {
        return ResponseEntity.ok(service.resolverComentario(id, comentarioId));
    }

    // ── Bibliografía ─────────────────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @PostMapping("/{id}/bibliografia")
    public ResponseEntity<ReferenciaBibliograficaResponse> crearReferencia(
            @PathVariable Long id, @RequestBody ReferenciaBibliograficaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bibliografiaService.crear(id, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @PutMapping("/{id}/bibliografia/{idReferencia}")
    public ResponseEntity<ReferenciaBibliograficaResponse> actualizarReferencia(
            @PathVariable Long id, @PathVariable Long idReferencia, @RequestBody ReferenciaBibliograficaRequest req) {
        return ResponseEntity.ok(bibliografiaService.actualizar(id, idReferencia, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @DeleteMapping("/{id}/bibliografia/{idReferencia}")
    public ResponseEntity<Void> eliminarReferencia(@PathVariable Long id, @PathVariable Long idReferencia) {
        bibliografiaService.eliminar(id, idReferencia);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/bibliografia")
    public ResponseEntity<List<ReferenciaBibliograficaResponse>> listarReferencias(@PathVariable Long id) {
        return ResponseEntity.ok(bibliografiaService.listar(id));
    }

    @GetMapping("/{id}/bibliografia/formateada")
    public ResponseEntity<BibliografiaFormateadaResponse> bibliografiaFormateada(
            @PathVariable Long id, @RequestParam(required = false) String estandar) {
        return ResponseEntity.ok(bibliografiaService.formatear(id, estandar));
    }

    // ── Exportación ──────────────────────────────────────────────────
    @PostMapping("/{id}/exportar-pdf")
    public ResponseEntity<byte[]> exportarPdf(@PathVariable Long id) {
        byte[] pdf = exportacionService.exportarPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=documento-" + id + ".pdf")
                .body(pdf);
    }

    @PostMapping("/{id}/exportar-docx")
    public ResponseEntity<byte[]> exportarDocx(@PathVariable Long id) {
        byte[] docx = exportacionService.exportarDocx(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=documento-" + id + ".docx")
                .body(docx);
    }
}
