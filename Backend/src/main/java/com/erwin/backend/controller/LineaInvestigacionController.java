package com.erwin.backend.controller;

import com.erwin.backend.dtos.LineaInvestigacionRequest;
import com.erwin.backend.dtos.LineaInvestigacionResponse;
import com.erwin.backend.service.LineaInvestigacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lineas-investigacion")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO','DIRECCION_ACADEMICA')")
public class LineaInvestigacionController {

    private final LineaInvestigacionService lineaService;

    public LineaInvestigacionController(LineaInvestigacionService lineaService) {
        this.lineaService = lineaService;
    }

    @GetMapping("/carrera/{carreraId}")
    public ResponseEntity<List<LineaInvestigacionResponse>> listarPorCarrera(@PathVariable Integer carreraId) {
        return ResponseEntity.ok(lineaService.listarPorCarrera(carreraId));
    }

    @PostMapping
    public ResponseEntity<LineaInvestigacionResponse> crear(@RequestBody LineaInvestigacionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lineaService.crear(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LineaInvestigacionResponse> actualizar(
            @PathVariable Long id, @RequestBody LineaInvestigacionRequest req) {
        return ResponseEntity.ok(lineaService.actualizar(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        lineaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
