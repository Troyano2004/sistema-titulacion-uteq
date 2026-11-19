package com.erwin.backend.controller;

import com.erwin.backend.dtos.JustificacionDtos.*;
import com.erwin.backend.service.JustificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/justificaciones")
@CrossOrigin(origins = "http://localhost:4200")
public class JustificacionController {

    private final JustificacionService service;

    public JustificacionController(JustificacionService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @PostMapping("/solicitar")
    public ResponseEntity<SolicitudJustificacionDto> solicitar(@RequestBody SolicitarJustificacionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.solicitar(req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','DECANO','COORDINADOR','DIRECCION_ACADEMICA')")
    @GetMapping("/estudiante/{id}")
    public ResponseEntity<List<SolicitudJustificacionDto>> historial(@PathVariable Integer id) {
        return ResponseEntity.ok(service.historial(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','DECANO')")
    @GetMapping("/pendientes")
    public ResponseEntity<List<SolicitudJustificacionDto>> pendientes() {
        return ResponseEntity.ok(service.pendientes());
    }

    @PreAuthorize("hasAnyRole('ADMIN','DECANO')")
    @PutMapping("/{id}/resolver")
    public ResponseEntity<SolicitudJustificacionDto> resolver(
            @PathVariable Long id, @RequestBody ResolverJustificacionRequest req) {
        return ResponseEntity.ok(service.resolver(id, req));
    }
}
