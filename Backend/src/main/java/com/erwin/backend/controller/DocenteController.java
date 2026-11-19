package com.erwin.backend.controller;

import com.erwin.backend.entities.Docente;
import com.erwin.backend.repository.DocenteRepository;
import com.erwin.backend.service.DocenteCarreraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/docentes")
@CrossOrigin(origins = "http://localhost:4200")
public class DocenteController {

    public record DocenteSimpleDto(Integer idDocente, String nombreCompleto, String cedula) {}
    public record AsignarCarreraRequest(Integer idCarrera) {}

    private final DocenteRepository docenteRepo;
    private final DocenteCarreraService docenteCarreraService;

    public DocenteController(DocenteRepository docenteRepo, DocenteCarreraService docenteCarreraService) {
        this.docenteRepo = docenteRepo;
        this.docenteCarreraService = docenteCarreraService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DECANO')")
    public List<DocenteSimpleDto> listar() {
        return docenteRepo.findAll().stream()
                .map(this::toSimpleDto)
                .sorted((a, b) -> a.nombreCompleto().compareToIgnoreCase(b.nombreCompleto()))
                .toList();
    }

    /** Para la pantalla de administración Docente↔Carrera: todos los docentes ya con sus carreras. */
    @GetMapping("/con-carreras")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
    public List<DocenteCarreraService.DocenteConCarrerasDto> listarConCarreras() {
        return docenteCarreraService.listarTodosConCarreras();
    }

    // Lectura de las carreras de un docente: cualquier rol de comisión/coordinación
    // puede necesitarla (p. ej. para auto-seleccionar carrera al registrar un tema).
    @GetMapping("/{idDocente}/carreras")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DOCENTE','DOCENTE_TITULADO','DECANO')")
    public List<DocenteCarreraService.CarreraSimpleDto> carrerasDeDocente(@PathVariable Integer idDocente) {
        return docenteCarreraService.listarCarrerasDeDocente(idDocente);
    }

    @PostMapping("/{idDocente}/carreras")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
    public ResponseEntity<?> asignarCarrera(@PathVariable Integer idDocente, @RequestBody AsignarCarreraRequest req) {
        if (req == null || req.idCarrera() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe indicar idCarrera"));
        }
        docenteCarreraService.asignarCarrera(idDocente, req.idCarrera());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(docenteCarreraService.listarCarrerasDeDocente(idDocente));
    }

    @DeleteMapping("/{idDocente}/carreras/{idCarrera}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
    public ResponseEntity<Void> quitarCarrera(@PathVariable Integer idDocente, @PathVariable Integer idCarrera) {
        docenteCarreraService.quitarCarrera(idDocente, idCarrera);
        return ResponseEntity.noContent().build();
    }

    private DocenteSimpleDto toSimpleDto(Docente d) {
        String nombre = d.getUsuario() != null
                ? (d.getUsuario().getNombres() + " " + d.getUsuario().getApellidos()).trim()
                : ("Docente " + d.getIdDocente());
        String cedula = d.getUsuario() != null ? d.getUsuario().getCedula() : null;
        return new DocenteSimpleDto(d.getIdDocente(), nombre, cedula);
    }
}
