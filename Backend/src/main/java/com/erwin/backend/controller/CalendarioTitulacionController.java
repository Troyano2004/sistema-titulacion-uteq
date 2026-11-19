package com.erwin.backend.controller;

import com.erwin.backend.dtos.CalendarioDtos.*;
import com.erwin.backend.service.CalendarioTitulacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calendario-titulacion")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO','DIRECCION_ACADEMICA','DOCENTE','DOCENTE_TITULADO')")
public class CalendarioTitulacionController {

    private final CalendarioTitulacionService service;

    public CalendarioTitulacionController(CalendarioTitulacionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<EventoCalendarioDto>> obtenerEventos(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) Integer facultad) {
        return ResponseEntity.ok(service.obtenerEventos(periodo, facultad));
    }

    @GetMapping("/periodo/{idPeriodo}/aval")
    public ResponseEntity<PeriodoAvalDto> obtenerAval(@PathVariable Integer idPeriodo) {
        return ResponseEntity.ok(service.obtenerAval(idPeriodo));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO','DIRECCION_ACADEMICA')")
    @PutMapping("/periodo/{idPeriodo}/aval")
    public ResponseEntity<PeriodoAvalDto> darAval(
            @PathVariable Integer idPeriodo, @RequestBody AvalUnidadRegistroRequest req) {
        return ResponseEntity.ok(service.darAval(idPeriodo, req));
    }
}
