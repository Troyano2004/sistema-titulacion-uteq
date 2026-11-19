package com.erwin.backend.controller;

import com.erwin.backend.entities.FeriadoAcademico;
import com.erwin.backend.repository.FeriadoAcademicoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * CRUD mínimo para los feriados que excluyen las utilidades de días/horas
 * hábiles (Módulo 14, TAREA 2.3) — sin esto la exclusión de feriados sería
 * código muerto porque no habría cómo poblar la tabla.
 */
@RestController
@RequestMapping("/api/feriados-academicos")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO','DIRECCION_ACADEMICA')")
public class FeriadoAcademicoController {

    private final FeriadoAcademicoRepository repo;

    public FeriadoAcademicoController(FeriadoAcademicoRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<List<FeriadoAcademico>> listar() {
        return ResponseEntity.ok(repo.findAllByOrderByFechaAsc());
    }

    @PostMapping
    public ResponseEntity<FeriadoAcademico> crear(@RequestBody FeriadoAcademico feriado) {
        if (feriado.getFecha() == null || feriado.getDescripcion() == null || feriado.getDescripcion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha y descripción son obligatorias");
        }
        feriado.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(feriado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
