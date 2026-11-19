// Proyectofindecurso/Backend/src/main/java/com/erwin/backend/controller/ProyectoTitulacionController.java
package com.erwin.backend.controller;

import com.erwin.backend.dtos.ProyectoTitulacionCreateRequest;
import com.erwin.backend.entities.ProyectoTitulacion;
import com.erwin.backend.service.ProyectoTitulacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/proyectos-titulacion")
@PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DOCENTE','DOCENTE_TITULADO')")
public class ProyectoTitulacionController {

    private final ProyectoTitulacionService proyectoService;

    public ProyectoTitulacionController(ProyectoTitulacionService proyectoService) {
        this.proyectoService = proyectoService;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ProyectoTitulacionCreateRequest req) {
        try {
            ProyectoTitulacion creado = proyectoService.crearProyecto(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    /** Resuelve el proyecto vigente de un estudiante — usado por el selector de estudiante en Reportes/Actas. */
    @GetMapping("/estudiante/{idEstudiante}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DOCENTE','DOCENTE_TITULADO','DECANO','DIRECCION_ACADEMICA','ROL_REPORT')")
    public ResponseEntity<?> buscarPorEstudiante(@PathVariable Integer idEstudiante) {
        return proyectoService.buscarPorEstudiante(idEstudiante)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(Map.of(
                        "idProyecto", p.getIdProyecto(),
                        "titulo", p.getTitulo() != null ? p.getTitulo() : "",
                        "estado", p.getEstado() != null ? p.getEstado() : ""
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
