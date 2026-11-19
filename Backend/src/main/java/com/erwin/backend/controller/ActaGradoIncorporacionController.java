package com.erwin.backend.controller;

import com.erwin.backend.dtos.IncorporacionDtos.*;
import com.erwin.backend.service.ActaGradoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/actas-grado")
@CrossOrigin(origins = "http://localhost:4200")
public class ActaGradoIncorporacionController {

    private final ActaGradoService service;

    public ActaGradoIncorporacionController(ActaGradoService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIO_ABOGADO','DIRECCION_ACADEMICA','DECANO')")
    @PostMapping("/generar/{estudianteId}")
    public ResponseEntity<ActaGradoIncorporacionResponse> generar(@PathVariable Integer estudianteId) {
        return ResponseEntity.ok(service.generar(estudianteId));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIO_ABOGADO','DIRECCION_ACADEMICA','DECANO')")
    @PutMapping("/{id}/firmar")
    public ResponseEntity<ActaGradoIncorporacionResponse> firmar(
            @PathVariable Integer id, @RequestBody FirmarActaRequest req) {
        return ResponseEntity.ok(service.firmar(id, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIO_ABOGADO','DIRECCION_ACADEMICA')")
    @PutMapping("/{id}/ceremonia")
    public ResponseEntity<ActaGradoIncorporacionResponse> programarCeremonia(
            @PathVariable Integer id, @RequestBody ProgramarCeremoniaRequest req) {
        return ResponseEntity.ok(service.programarCeremonia(id, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','SECRETARIO_ABOGADO','DIRECCION_ACADEMICA','DECANO','COORDINADOR')")
    @GetMapping("/estudiante/{idEstudiante}")
    public ResponseEntity<ActaGradoIncorporacionResponse> obtenerPorEstudiante(@PathVariable Integer idEstudiante) {
        return ResponseEntity.ok(service.obtenerPorEstudiante(idEstudiante));
    }
}
