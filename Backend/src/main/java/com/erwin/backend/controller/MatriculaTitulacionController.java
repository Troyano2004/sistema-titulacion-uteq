package com.erwin.backend.controller;

import com.erwin.backend.dtos.CambiarOpcionMatriculaRequest;
import com.erwin.backend.dtos.MatriculaTitulacionRequest;
import com.erwin.backend.dtos.MatriculaTitulacionResponse;
import com.erwin.backend.service.MatriculaTitulacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matriculas-titulacion")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','SECRETARIO_ABOGADO','DECANO','DIRECCION_ACADEMICA')")
public class MatriculaTitulacionController {

    private final MatriculaTitulacionService matriculaService;

    public MatriculaTitulacionController(MatriculaTitulacionService matriculaService) {
        this.matriculaService = matriculaService;
    }

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<List<MatriculaTitulacionResponse>> listarPorEstudiante(@PathVariable Integer estudianteId) {
        return ResponseEntity.ok(matriculaService.listarPorEstudiante(estudianteId));
    }

    @PostMapping
    public ResponseEntity<MatriculaTitulacionResponse> crear(@RequestBody MatriculaTitulacionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(matriculaService.crear(req));
    }

    @PutMapping("/{id}/cambiar-opcion")
    public ResponseEntity<MatriculaTitulacionResponse> cambiarOpcion(
            @PathVariable Long id, @RequestBody CambiarOpcionMatriculaRequest req) {
        return ResponseEntity.ok(matriculaService.cambiarOpcion(id, req));
    }
}
