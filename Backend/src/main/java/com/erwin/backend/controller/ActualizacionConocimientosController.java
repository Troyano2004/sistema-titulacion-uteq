package com.erwin.backend.controller;

import com.erwin.backend.dtos.OportunidadDtos.*;
import com.erwin.backend.service.ActualizacionConocimientosService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actualizacion-conocimientos")
@CrossOrigin(origins = "http://localhost:4200")
public class ActualizacionConocimientosController {

    private final ActualizacionConocimientosService service;

    public ActualizacionConocimientosController(ActualizacionConocimientosService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','COORDINADOR')")
    @PostMapping("/solicitar")
    public ResponseEntity<ActualizacionConocimientosDto> solicitar(@RequestBody SolicitarActualizacionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.solicitar(req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO')")
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<ActualizacionConocimientosDto> aprobar(
            @PathVariable Long id, @RequestBody(required = false) AprobarActualizacionRequest req) {
        AprobarActualizacionRequest efectivo = req != null
                ? new AprobarActualizacionRequest("APROBADA", req.resolucionConsejoDirectivo())
                : new AprobarActualizacionRequest("APROBADA", null);
        return ResponseEntity.ok(service.resolver(id, efectivo));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO')")
    @PutMapping("/{id}/rechazar")
    public ResponseEntity<ActualizacionConocimientosDto> rechazar(
            @PathVariable Long id, @RequestBody(required = false) AprobarActualizacionRequest req) {
        String resolucion = req != null ? req.resolucionConsejoDirectivo() : null;
        return ResponseEntity.ok(service.resolver(id, new AprobarActualizacionRequest("RECHAZADA", resolucion)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO','ESTUDIANTE')")
    @GetMapping("/estudiante/{id}")
    public ResponseEntity<List<ActualizacionConocimientosDto>> historial(@PathVariable Integer id) {
        return ResponseEntity.ok(service.historial(id));
    }
}
