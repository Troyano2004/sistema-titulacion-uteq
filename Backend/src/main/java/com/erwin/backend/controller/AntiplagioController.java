package com.erwin.backend.controller;

import com.erwin.backend.dtos.AntiplagioResultadoResponse;
import com.erwin.backend.dtos.CertificadoCompilatioResponse;
import com.erwin.backend.dtos.RegistrarResultadoAntiplagioRequest;
import com.erwin.backend.service.AntiplagioCompilatioService;
import com.erwin.backend.service.AntiplagioIaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/antiplagio")
@CrossOrigin(origins = "http://localhost:4200")
public class AntiplagioController {

    private final AntiplagioIaService service;
    private final AntiplagioCompilatioService compilatioService;

    public AntiplagioController(AntiplagioIaService service, AntiplagioCompilatioService compilatioService) {
        this.service = service;
        this.compilatioService = compilatioService;
    }

    // ── Módulo 8: resultado COMPILATIO (manual, el Director escribe el %) ──

    @PostMapping("/registrar-resultado")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    public ResponseEntity<AntiplagioResultadoResponse> registrarResultado(
            @RequestBody RegistrarResultadoAntiplagioRequest req) {
        return ResponseEntity.ok(compilatioService.registrarResultado(req));
    }

    @PostMapping("/emitir-certificado/{estudianteId}")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    public ResponseEntity<CertificadoCompilatioResponse> emitirCertificado(@PathVariable Integer estudianteId) {
        return ResponseEntity.ok(compilatioService.emitirCertificado(estudianteId));
    }

    /**
     * Reemplaza la llamada directa que el frontend hacía a WasItAIGenerated
     * (antiplagio-dt2.ts) con la API key hardcodeada en el bundle. Ahora el
     * frontend solo envía el texto; la key vive en el servidor.
     */
    @PostMapping("/verificar-texto")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    public ResponseEntity<String> verificarTexto(@RequestBody VerificarTextoRequest req) {
        String texto = req == null ? null : req.getTexto();
        return service.verificarTexto(texto);
    }

    public static class VerificarTextoRequest {
        private String texto;

        public String getTexto() { return texto; }
        public void setTexto(String texto) { this.texto = texto; }
    }
}
