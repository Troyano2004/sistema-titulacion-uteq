package com.erwin.backend.controller;

import com.erwin.backend.dtos.BancoPreguntasDtos.*;
import com.erwin.backend.service.BancoPreguntasService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Banco de preguntas del Examen Complexivo. Todos los endpoints exigen rol
 * docente/coordinador — el estudiante NUNCA tiene acceso a este banco
 * (Módulo 12, TAREA 2.3).
 */
@RestController
@RequestMapping("/api/banco-preguntas")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DOCENTE','DOCENTE_TITULADO')")
public class BancoPreguntasController {

    private final BancoPreguntasService service;

    public BancoPreguntasController(BancoPreguntasService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PreguntaComplexivoResponse> crear(@RequestBody PreguntaComplexivoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PreguntaComplexivoResponse> actualizar(
            @PathVariable Long id, @RequestBody PreguntaComplexivoRequest req) {
        return ResponseEntity.ok(service.actualizar(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<PreguntaComplexivoResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(service.desactivar(id));
    }

    @GetMapping("/carrera/{carreraId}")
    public ResponseEntity<List<PreguntaComplexivoResponse>> listar(
            @PathVariable Integer carreraId,
            @RequestParam(required = false) Long ejeTematico,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(service.listar(carreraId, ejeTematico, estado));
    }

    @GetMapping("/carrera/{carreraId}/estadisticas")
    public ResponseEntity<EstadisticasBancoDto> estadisticas(@PathVariable Integer carreraId) {
        return ResponseEntity.ok(service.estadisticas(carreraId));
    }
}
