package com.erwin.backend.controller;

import com.erwin.backend.dtos.OportunidadDtos.*;
import com.erwin.backend.enums.TipoOportunidad;
import com.erwin.backend.service.OportunidadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/oportunidades")
@CrossOrigin(origins = "http://localhost:4200")
public class OportunidadController {

    private final OportunidadService service;

    public OportunidadController(OportunidadService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO','DIRECCION_ACADEMICA','DOCENTE','DOCENTE_TITULADO','ESTUDIANTE')")
    @GetMapping("/estudiante/{id}")
    public ResponseEntity<List<RegistroOportunidadDto>> historial(@PathVariable Integer id) {
        return ResponseEntity.ok(service.historial(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO','DIRECCION_ACADEMICA','DOCENTE','DOCENTE_TITULADO','ESTUDIANTE')")
    @GetMapping("/estudiante/{id}/puede-rendir")
    public ResponseEntity<ElegibilidadDto> puedeRendir(@PathVariable Integer id, @RequestParam String tipo) {
        TipoOportunidad tipoOportunidad;
        try {
            tipoOportunidad = TipoOportunidad.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo inválido: " + tipo);
        }
        return ResponseEntity.ok(service.puedeRendir(id, tipoOportunidad));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO')")
    @PostMapping("/estudiante/{id}/autorizar-tercera-matricula")
    public ResponseEntity<Void> autorizarTerceraMatricula(
            @PathVariable Integer id, @RequestBody AutorizarTerceraMatriculaRequest req) {
        service.autorizarTerceraMatricula(new AutorizarTerceraMatriculaRequest(id, req.resolucion(), req.idUsuarioAutoriza()));
        return ResponseEntity.noContent().build();
    }
}
