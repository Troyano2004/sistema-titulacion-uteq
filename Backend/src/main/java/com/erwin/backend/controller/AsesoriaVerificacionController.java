package com.erwin.backend.controller;

import com.erwin.backend.dtos.VerificacionMensualResponse;
import com.erwin.backend.service.AsesoriaVerificacionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asesorias")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DOCENTE','DOCENTE_TITULADO')")
public class AsesoriaVerificacionController {

    private final AsesoriaVerificacionService service;

    public AsesoriaVerificacionController(AsesoriaVerificacionService service) {
        this.service = service;
    }

    @GetMapping("/verificar-cumplimiento")
    public VerificacionMensualResponse verificarCumplimiento(
            @RequestParam Integer estudianteId,
            @RequestParam Integer mes,
            @RequestParam Integer anio) {
        return service.verificarCumplimiento(estudianteId, mes, anio);
    }
}
