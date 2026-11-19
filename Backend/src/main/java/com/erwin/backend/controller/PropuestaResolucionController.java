package com.erwin.backend.controller;

import com.erwin.backend.dtos.*;
import com.erwin.backend.service.AnteproyectoService;
import com.erwin.backend.service.PropuestaResolucionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/propuestas")
@CrossOrigin(origins = "http://localhost:4200")
public class PropuestaResolucionController {

    private final PropuestaResolucionService resolucionService;
    private final AnteproyectoService anteproyectoService;

    public PropuestaResolucionController(PropuestaResolucionService resolucionService,
                                          AnteproyectoService anteproyectoService) {
        this.resolucionService = resolucionService;
        this.anteproyectoService = anteproyectoService;
    }

    @GetMapping("/pendientes-consejo-directivo")
    @PreAuthorize("hasAnyRole('DECANO','COORDINADOR')")
    public ResponseEntity<List<PropuestaPendienteResponse>> pendientes(
            @RequestParam(required = false) Integer idCarrera) {
        return ResponseEntity.ok(resolucionService.listarPendientes(idCarrera));
    }

    @GetMapping("/docentes-elegibles")
    @PreAuthorize("hasAnyRole('DECANO','COORDINADOR')")
    public ResponseEntity<List<DocenteElegibleResponse>> docentesElegibles(@RequestParam Integer idCarrera) {
        return ResponseEntity.ok(resolucionService.listarDocentesElegibles(idCarrera));
    }

    @PostMapping("/{id}/aprobar-y-designar")
    @PreAuthorize("hasAnyRole('DECANO','COORDINADOR')")
    public ResponseEntity<AprobarYDesignarResponse> aprobarYDesignar(
            @PathVariable Integer id, @RequestBody AprobarYDesignarRequest req) {
        return ResponseEntity.ok(resolucionService.aprobarYDesignar(id, req));
    }

    @PostMapping("/{id}/solicitar-modificacion")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    public ResponseEntity<AnteproyectoModificacionResponse> solicitarModificacion(
            @PathVariable Integer id, @RequestBody SolicitarModificacionRequest req) {
        return ResponseEntity.ok(anteproyectoService.solicitarModificacion(id, req));
    }

    @PutMapping("/{id}/aprobar-modificacion")
    @PreAuthorize("hasAnyRole('DECANO','COORDINADOR')")
    public ResponseEntity<AnteproyectoVersionResponse> aprobarModificacion(
            @PathVariable Integer id, @RequestBody AprobarModificacionRequest req) {
        return ResponseEntity.ok(anteproyectoService.aprobarModificacion(id, req));
    }
}
