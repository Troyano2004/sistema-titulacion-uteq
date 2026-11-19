package com.erwin.backend.controller;

import com.erwin.backend.dtos.SgaImportDtos.*;
import com.erwin.backend.service.SgaImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Módulo 20 — importación manual de datos del SGA (puente temporal, ver
 * SgaImportService). Solo ADMIN y COORDINADOR pueden importar (REGLAS).
 */
@RestController
@RequestMapping("/api/sga")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
public class SgaImportController {

    private final SgaImportService service;

    public SgaImportController(SgaImportService service) {
        this.service = service;
    }

    @PostMapping(value = "/importar-pensum", consumes = "multipart/form-data")
    public ResponseEntity<ImportacionResultadoDto> importarPensum(
            @RequestParam("file") MultipartFile file, @RequestParam(required = false) Integer idUsuarioImporta) {
        return ResponseEntity.ok(service.importarPensum(file, idUsuarioImporta));
    }

    @PostMapping(value = "/importar-requisitos", consumes = "multipart/form-data")
    public ResponseEntity<ImportacionResultadoDto> importarRequisitos(
            @RequestParam("file") MultipartFile file, @RequestParam(required = false) Integer idUsuarioImporta) {
        return ResponseEntity.ok(service.importarRequisitos(file, idUsuarioImporta));
    }

    @PostMapping(value = "/importar-deudas", consumes = "multipart/form-data")
    public ResponseEntity<ImportacionResultadoDto> importarDeudas(
            @RequestParam("file") MultipartFile file, @RequestParam(required = false) Integer idUsuarioImporta) {
        return ResponseEntity.ok(service.importarDeudas(file, idUsuarioImporta));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<ImportacionLogDto>> historial() {
        return ResponseEntity.ok(service.historial());
    }
}
