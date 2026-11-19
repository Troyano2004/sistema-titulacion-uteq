package com.erwin.backend.controller;

import com.erwin.backend.dtos.ExamenComplexivoDtos.*;
import com.erwin.backend.service.ExamenComplexivoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examen-complexivo")
@CrossOrigin(origins = "http://localhost:4200")
public class ExamenComplexivoController {

    private final ExamenComplexivoService service;

    public ExamenComplexivoController(ExamenComplexivoService service) {
        this.service = service;
    }

    // ── Dirección Académica / Coordinación ───────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DIRECCION_ACADEMICA')")
    @PostMapping("/programar")
    public ResponseEntity<ExamenAplicacionResponse> programar(@RequestBody ProgramarExamenRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.programar(req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DIRECCION_ACADEMICA')")
    @GetMapping("/delegados-disponibles")
    public ResponseEntity<List<DelegadoDisponibleDto>> delegadosDisponibles(
            @RequestParam Integer carreraEstudiante) {
        return ResponseEntity.ok(service.delegadosDisponibles(carreraEstudiante));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DIRECCION_ACADEMICA')")
    @PostMapping("/{id}/asignar-delegado")
    public ResponseEntity<ExamenAplicacionResponse> asignarDelegado(
            @PathVariable Long id, @RequestBody AsignarDelegadoRequest req) {
        return ResponseEntity.ok(service.asignarDelegado(id, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DIRECCION_ACADEMICA','DOCENTE','DOCENTE_TITULADO')")
    @GetMapping("/{id}")
    public ResponseEntity<ExamenAplicacionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DIRECCION_ACADEMICA','ESTUDIANTE')")
    @GetMapping("/estudiante/{idEstudiante}")
    public ResponseEntity<List<ExamenAplicacionResponse>> listarPorEstudiante(@PathVariable Integer idEstudiante) {
        return ResponseEntity.ok(service.listarPorEstudiante(idEstudiante));
    }

    // ── Acta de ingreso / fraude — supervisor (DOCENTE o delegado) ───
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO','ESTUDIANTE')")
    @PostMapping("/{id}/registrar-ingreso")
    public ResponseEntity<ExamenAplicacionResponse> registrarIngreso(
            @PathVariable Long id, @RequestBody RegistrarIngresoRequest req) {
        return ResponseEntity.ok(service.registrarIngreso(id, req));
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/{id}/registrar-fraude")
    public ResponseEntity<ExamenAplicacionResponse> registrarFraude(
            @PathVariable Long id, @RequestBody RegistrarFraudeRequest req) {
        return ResponseEntity.ok(service.registrarFraude(id, req));
    }

    // ── Estudiante — rendir el examen ─────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @PostMapping("/{id}/iniciar")
    public ResponseEntity<IniciarExamenResponse> iniciar(@PathVariable Long id) {
        return ResponseEntity.ok(service.iniciar(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @GetMapping("/{id}/preguntas-asignadas")
    public ResponseEntity<IniciarExamenResponse> preguntasAsignadas(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPreguntasAsignadas(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @PutMapping("/{id}/responder")
    public ResponseEntity<Void> responder(@PathVariable Long id, @RequestBody ResponderPreguntaRequest req) {
        service.responder(id, req);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @PostMapping("/{id}/finalizar")
    public ResponseEntity<ResultadoExamenDto> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(service.finalizar(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE','COORDINADOR','DIRECCION_ACADEMICA')")
    @GetMapping("/{id}/resultado")
    public ResponseEntity<ResultadoExamenDto> resultado(@PathVariable Long id) {
        return ResponseEntity.ok(service.resultado(id));
    }
}
