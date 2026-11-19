package com.erwin.backend.controller;

import com.erwin.backend.dtos.ConfiguracionModalidadEntregaRequest;
import com.erwin.backend.dtos.ConfiguracionModalidadEntregaResponse;
import com.erwin.backend.service.ConfiguracionTitulacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuracion-titulacion")
@CrossOrigin(origins = "http://localhost:4200")
public class ConfiguracionTitulacionController {

    private final ConfiguracionTitulacionService service;

    public ConfiguracionTitulacionController(ConfiguracionTitulacionService service) {
        this.service = service;
    }

    @PutMapping("/modalidad-entrega")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    public ResponseEntity<ConfiguracionModalidadEntregaResponse> configurar(
            @RequestBody ConfiguracionModalidadEntregaRequest req) {
        return ResponseEntity.ok(service.configurar(req));
    }

    @GetMapping("/modalidad-entrega")
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO','ESTUDIANTE','COORDINADOR')")
    public ResponseEntity<ConfiguracionModalidadEntregaResponse> obtener(
            @RequestParam Integer idCarrera, @RequestParam Integer idPeriodo) {
        return ResponseEntity.ok(service.obtener(idCarrera, idPeriodo));
    }
}
