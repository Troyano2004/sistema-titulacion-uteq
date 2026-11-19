package com.erwin.backend.controller;


import com.erwin.backend.dtos.TutoriaHistorialResponse;
import com.erwin.backend.dtos.TutoriaPendienteResponse;
import com.erwin.backend.security.AuthorizationService;
import com.erwin.backend.service.TutoriaEstudianteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/estudiante/tutorias")
@PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','DOCENTE','DOCENTE_TITULADO')")
public class TutoriaEstudianteController {

    private final TutoriaEstudianteService service;
    private final AuthorizationService authorizationService;

    public TutoriaEstudianteController(TutoriaEstudianteService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/historial/{idEstudiante}/{idAnteproyecto}")
    public List<TutoriaHistorialResponse> historial(
            @PathVariable Integer idEstudiante,
            @PathVariable Integer idAnteproyecto
    ) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        return service.historial(idEstudiante, idAnteproyecto);
    }

    @GetMapping("/reuniones/{idEstudiante}")
    public List<TutoriaPendienteResponse> reuniones(@PathVariable Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        return service.Reuniones(idEstudiante);
    }

}