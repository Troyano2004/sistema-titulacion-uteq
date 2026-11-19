package com.erwin.backend.controller;

import com.erwin.backend.dtos.ListadoSeleccionesResponse;
import com.erwin.backend.service.TutoriaListadoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tutorias")
@CrossOrigin(origins = "http://localhost:4200")
@PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO','DIRECCION_ACADEMICA')")
public class TutoriaListadoController {

    private final TutoriaListadoService service;

    public TutoriaListadoController(TutoriaListadoService service) {
        this.service = service;
    }

    @GetMapping("/listado-selecciones")
    public ListadoSeleccionesResponse listadoSelecciones(@RequestParam Integer carreraId,
                                                          @RequestParam(required = false) String periodo) {
        return service.listado(carreraId, periodo);
    }
}
