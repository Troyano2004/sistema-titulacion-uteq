package com.erwin.backend.controller;

import com.erwin.backend.dtos.IncorporacionDtos.PendienteIncorporacionDto;
import com.erwin.backend.service.ActaGradoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incorporacion")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('ADMIN','SECRETARIO_ABOGADO','DIRECCION_ACADEMICA','DECANO','COORDINADOR')")
public class IncorporacionController {

    private final ActaGradoService actaGradoService;

    public IncorporacionController(ActaGradoService actaGradoService) {
        this.actaGradoService = actaGradoService;
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<PendienteIncorporacionDto>> pendientes(@RequestParam(required = false) Integer facultad) {
        return ResponseEntity.ok(actaGradoService.pendientes(facultad));
    }
}
