package com.erwin.backend.controller;

import com.erwin.backend.dtos.EjeTematicoRequest;
import com.erwin.backend.dtos.EjeTematicoResponse;
import com.erwin.backend.dtos.PlanificacionEjeRequest;
import com.erwin.backend.service.EjeTematicoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ejes-tematicos")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO','DIRECCION_ACADEMICA')")
public class EjeTematicoController {

    private final EjeTematicoService ejeService;

    public EjeTematicoController(EjeTematicoService ejeService) {
        this.ejeService = ejeService;
    }

    @GetMapping("/carrera/{carreraId}")
    public ResponseEntity<List<EjeTematicoResponse>> listarPorCarrera(
            @PathVariable Integer carreraId,
            @RequestParam(required = false) String periodo) {
        return ResponseEntity.ok(ejeService.listarPorCarrera(carreraId, periodo));
    }

    @PostMapping
    public ResponseEntity<EjeTematicoResponse> crear(@RequestBody EjeTematicoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ejeService.crear(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EjeTematicoResponse> actualizar(@PathVariable Long id, @RequestBody EjeTematicoRequest req) {
        return ResponseEntity.ok(ejeService.actualizar(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ejeService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/planificacion")
    public ResponseEntity<EjeTematicoResponse> planificar(
            @PathVariable Long id, @RequestBody PlanificacionEjeRequest req) {
        return ResponseEntity.ok(ejeService.planificar(id, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO')")
    @PutMapping("/{id}/aprobar-planificacion")
    public ResponseEntity<EjeTematicoResponse> aprobarPlanificacion(
            @PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer idUsuarioAprobador = body.get("idUsuarioAprobador");
        return ResponseEntity.ok(ejeService.aprobarPlanificacion(id, idUsuarioAprobador));
    }

    @GetMapping("/carrera/{carreraId}/con-docentes")
    public ResponseEntity<List<EjeTematicoResponse>> listarConDocentes(@PathVariable Integer carreraId) {
        return ResponseEntity.ok(ejeService.listarConDocentes(carreraId));
    }
}
