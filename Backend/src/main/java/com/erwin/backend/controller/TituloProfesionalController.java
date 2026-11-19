package com.erwin.backend.controller;

import com.erwin.backend.dtos.IncorporacionDtos.*;
import com.erwin.backend.service.TituloProfesionalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/titulos")
@CrossOrigin(origins = "http://localhost:4200")
public class TituloProfesionalController {

    private final TituloProfesionalService service;

    public TituloProfesionalController(TituloProfesionalService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIO_ABOGADO','DIRECCION_ACADEMICA')")
    @PostMapping("/emitir/{actaGradoId}")
    public ResponseEntity<TituloProfesionalResponse> emitir(
            @PathVariable Integer actaGradoId, @RequestBody EmitirTituloRequest req) {
        return ResponseEntity.ok(service.emitir(actaGradoId, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SECRETARIO_ABOGADO','DIRECCION_ACADEMICA')")
    @PutMapping("/{id}/registrar-senescyt")
    public ResponseEntity<TituloProfesionalResponse> registrarSenescyt(
            @PathVariable Long id, @RequestBody RegistrarSenescytRequest req) {
        return ResponseEntity.ok(service.registrarSenescyt(id, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','SECRETARIO_ABOGADO','DIRECCION_ACADEMICA','COORDINADOR')")
    @GetMapping("/estudiante/{idEstudiante}")
    public ResponseEntity<TituloProfesionalResponse> obtenerPorEstudiante(@PathVariable Integer idEstudiante) {
        return ResponseEntity.ok(service.obtenerPorEstudiante(idEstudiante));
    }
}
