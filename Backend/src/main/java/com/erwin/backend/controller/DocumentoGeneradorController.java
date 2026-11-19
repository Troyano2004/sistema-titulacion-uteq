package com.erwin.backend.controller;

import com.erwin.backend.security.AuthorizationService;
import com.erwin.backend.service.DocumentStorageService;
import com.erwin.backend.service.ExpedienteService;
import com.erwin.backend.service.GeneradorDocumentosService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;

/**
 * Módulo 16 — genera las 11 plantillas documentales del TIC como PDF y las
 * registra automáticamente en el expediente del estudiante (si existe).
 */
@RestController
@RequestMapping("/api/documentos/generar")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','DOCENTE','DOCENTE_TITULADO','COORDINADOR','SECRETARIO_ABOGADO','DECANO')")
public class DocumentoGeneradorController {

    private final GeneradorDocumentosService generador;
    private final ExpedienteService expedienteService;
    private final DocumentStorageService storageService;
    private final AuthorizationService authorizationService;

    public DocumentoGeneradorController(GeneradorDocumentosService generador,
                                         ExpedienteService expedienteService,
                                         DocumentStorageService storageService,
                                         AuthorizationService authorizationService) {
        this.generador = generador;
        this.expedienteService = expedienteService;
        this.storageService = storageService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/portada/{estudianteId}")
    public ResponseEntity<byte[]> portada(@PathVariable Integer estudianteId) {
        return generarYRegistrar(estudianteId, "portada", "A", generador::generarPortada);
    }

    @GetMapping("/autoria/{estudianteId}")
    public ResponseEntity<byte[]> autoria(@PathVariable Integer estudianteId) {
        return generarYRegistrar(estudianteId, "declaracion-autoria", "B", generador::generarDeclaracionAutoria);
    }

    @GetMapping("/certificacion-director/{estudianteId}")
    public ResponseEntity<byte[]> certificacionDirector(@PathVariable Integer estudianteId) {
        return generarYRegistrar(estudianteId, "certificacion-culminacion", "C", generador::generarCertificacionCulminacion);
    }

    @GetMapping("/certificado-antiplagio/{estudianteId}")
    public ResponseEntity<byte[]> certificadoAntiplagio(@PathVariable Integer estudianteId) {
        return generarYRegistrar(estudianteId, "certificado-antiplagio", "D", generador::generarCertificadoAntiplagio);
    }

    @GetMapping("/pagina-tribunal/{estudianteId}")
    public ResponseEntity<byte[]> paginaTribunal(@PathVariable Integer estudianteId) {
        return generarYRegistrar(estudianteId, "pagina-tribunal", "E", generador::generarPaginaTribunal);
    }

    @GetMapping("/codigo-dublin/{estudianteId}")
    public ResponseEntity<byte[]> codigoDublin(@PathVariable Integer estudianteId) {
        return generarYRegistrar(estudianteId, "codigo-dublin", "F", generador::generarCodigoDublin);
    }

    @GetMapping("/informe-favorable/{estudianteId}")
    public ResponseEntity<byte[]> informeFavorable(@PathVariable Integer estudianteId) {
        return generarYRegistrar(estudianteId, "informe-favorable-director", "G", generador::generarInformeFavorableDirector);
    }

    @GetMapping("/certificacion-tribunal/{estudianteId}")
    public ResponseEntity<byte[]> certificacionTribunal(@PathVariable Integer estudianteId) {
        return generarYRegistrar(estudianteId, "certificacion-tribunal", "H", generador::generarCertificacionTribunal);
    }

    @GetMapping("/certificado-redaccion/{estudianteId}")
    public ResponseEntity<byte[]> certificadoRedaccion(@PathVariable Integer estudianteId) {
        return generarYRegistrar(estudianteId, "certificado-redaccion-tecnica", "I", generador::generarCertificadoRedaccionTecnica);
    }

    @GetMapping("/acta-sustentacion/{estudianteId}")
    public ResponseEntity<byte[]> actaSustentacion(@PathVariable Integer estudianteId) {
        return generarYRegistrar(estudianteId, "acta-sustentacion", "J", generador::generarActaSustentacion);
    }

    @GetMapping("/acta-grado/{estudianteId}")
    public ResponseEntity<byte[]> actaGrado(@PathVariable Integer estudianteId) {
        return generarYRegistrar(estudianteId, "acta-grado", "K", generador::generarActaGrado);
    }

    // ── Helper compartido: genera, sube a blob, registra en expediente y descarga ──
    private ResponseEntity<byte[]> generarYRegistrar(Integer estudianteId, String nombreArchivo,
                                                       String codigoCatalogo, Function<Integer, byte[]> generar) {
        authorizationService.exigirPropioEstudianteSiAplica(estudianteId);
        byte[] pdf = generar.apply(estudianteId);
        try {
            String url = storageService.storeGeneratedPdf(pdf, nombreArchivo + "-" + estudianteId);
            expedienteService.registrarDocumentoGenerado(estudianteId, codigoCatalogo, url);
        } catch (Exception ex) {
            // No bloquear la descarga si Azure no está disponible o el expediente no existe aún.
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo + "-" + estudianteId + ".pdf")
                .body(pdf);
    }
}
