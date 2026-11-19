package com.erwin.backend.controller;

import com.erwin.backend.dtos.ExpedienteDtos.*;
import com.erwin.backend.service.ExpedienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expedientes")
@CrossOrigin(origins = "http://localhost:4200")
public class ExpedienteController {

    private final ExpedienteService service;

    public ExpedienteController(ExpedienteService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','COORDINADOR','SECRETARIO_ABOGADO')")
    @PostMapping
    public ResponseEntity<ExpedienteDto> crear(@RequestBody CrearExpedienteRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','COORDINADOR','SECRETARIO_ABOGADO','DECANO')")
    @GetMapping("/estudiante/{id}")
    public ResponseEntity<ExpedienteDto> obtenerPorEstudiante(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtenerPorEstudiante(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','COORDINADOR','SECRETARIO_ABOGADO','DECANO')")
    @GetMapping("/{id}")
    public ResponseEntity<ExpedienteDto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','SECRETARIO_ABOGADO','DECANO')")
    @GetMapping
    public ResponseEntity<List<ExpedienteDto>> listar(
            @RequestParam(required = false) Integer carrera,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(service.listar(carrera, periodo, estado));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @PutMapping("/documentos/{id}/cargar")
    public ResponseEntity<DocumentoExpedienteDto> cargar(@PathVariable Long id, @RequestBody CargarDocumentoRequest req) {
        return ResponseEntity.ok(service.cargar(id, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIO_ABOGADO')")
    @PutMapping("/documentos/{id}/validar")
    public ResponseEntity<DocumentoExpedienteDto> validar(@PathVariable Long id, @RequestBody ValidarDocumentoRequest req) {
        return ResponseEntity.ok(service.validar(id, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIO_ABOGADO')")
    @PutMapping("/documentos/{id}/rechazar")
    public ResponseEntity<DocumentoExpedienteDto> rechazar(@PathVariable Long id, @RequestBody RechazarDocumentoRequest req) {
        return ResponseEntity.ok(service.rechazar(id, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','COORDINADOR','SECRETARIO_ABOGADO','DECANO')")
    @GetMapping("/{id}/progreso")
    public ResponseEntity<ProgresoExpedienteDto> progreso(@PathVariable Long id) {
        return ResponseEntity.ok(service.progreso(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','COORDINADOR','SECRETARIO_ABOGADO','DECANO')")
    @GetMapping("/{id}/alertas")
    public ResponseEntity<List<AlertaExpedienteDto>> alertas(@PathVariable Long id) {
        return ResponseEntity.ok(service.alertas(id));
    }
}
