
package com.erwin.backend.controller;

import com.erwin.backend.dtos.ComplexivoDtos.*;
import com.erwin.backend.security.AuthorizationService;
import com.erwin.backend.service.InformePracticoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints del flujo de tutoría del informe práctico (Art. 20) — ver
 * {@link InformePracticoService}. Las rutas /api/complexivo/** se mantienen
 * sin cambios por compatibilidad con el frontend existente; el examen
 * complexivo real de 100 preguntas vive en ExamenComplexivoController
 * bajo /api/examen-complexivo/**.
 */
@RestController
@RequestMapping("/api/complexivo")
@CrossOrigin(origins = "http://localhost:4200")
public class ComplexivoController {

    private final InformePracticoService service;
    private final AuthorizationService authorizationService;

    public ComplexivoController(InformePracticoService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    // ── Módulo 17 — Nota de grado ─────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/{idEstudiante}/nota-grado")
    public NotaGradoComplexivoResponse calcularNotaGrado(
            @PathVariable Integer idEstudiante, @RequestBody NotaGradoComplexivoRequest req) {
        return service.calcularNotaGrado(idEstudiante, req);
    }

    // ── Coordinador DT1 ──────────────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
    @GetMapping("/coordinador/dt1/info")
    public InfoCoordinadorDt1Dto infoCoordinadorDt1(@RequestParam Integer idUsuario) {
        return service.infoCoordinadorDt1(idUsuario);
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
    @PostMapping("/coordinador/dt1/asignar")
    public ComplexivoDocenteAsignacionResponse asignarDt1(
            @RequestBody AsignarDt1ComplexivoRequest req) {
        return service.asignarDt1(req);
    }

    // ── Coordinador DT2 ──────────────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
    @GetMapping("/coordinador/dt2/info")
    public InfoCoordinadorDt2Dto infoCoordinadorDt2(@RequestParam Integer idUsuario) {
        return service.infoCoordinadorDt2(idUsuario);
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
    @PostMapping("/coordinador/dt2/asignar")
    public ComplexivoDocenteAsignacionResponse asignarDt2(
            @RequestBody AsignarDt2ComplexivoRequest req) {
        return service.asignarDt2(req);
    }

    // ── Endpoints legacy (compatibilidad con frontend existente) ─────
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
    @GetMapping("/coordinador/info")
    public InfoCoordinadorComplexivoDto infoCoordinador(@RequestParam Integer idUsuario) {
        return service.infoCoordinador(idUsuario);
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR')")
    @PostMapping("/coordinador/asignar-docente")
    public ComplexivoDocenteAsignacionResponse asignarDocente(
            @RequestBody AsignarDocenteComplexivoRequest req) {
        return service.asignarDocente(req);
    }

    // ── Estudiante ───────────────────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @GetMapping("/estudiante/{idEstudiante}/estado")
    public EstadoComplexivoEstudianteDto estadoEstudiante(
            @PathVariable Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        return service.estadoEstudiante(idEstudiante);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @GetMapping("/estudiante/{idEstudiante}/informe")
    public ComplexivoInformeDto getInforme(@PathVariable Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        return service.getInforme(idEstudiante);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @PutMapping("/estudiante/{idEstudiante}/informe")
    public ComplexivoInformeDto guardarInforme(
            @PathVariable Integer idEstudiante,
            @RequestBody ComplexivoInformeUpdateRequest req) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        return service.guardarInforme(idEstudiante, req);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @PostMapping("/estudiante/{idEstudiante}/informe/enviar")
    public ComplexivoInformeDto enviarInforme(@PathVariable Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        return service.enviarInforme(idEstudiante);
    }

    @PreAuthorize("hasAnyRole('ADMIN','ESTUDIANTE')")
    @PostMapping("/estudiante/{idEstudiante}/iniciar")
    public EstadoComplexivoEstudianteDto iniciarComplexivo(
            @PathVariable Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        return service.iniciarComplexivoDesdeEleccion(idEstudiante);
    }

    // ── DT1 — propuestas ─────────────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DOCENTE','DOCENTE_TITULADO')")
    @GetMapping("/dt1/{idDocente}/propuestas")
    public List<PropuestaComplexivoDto> propuestasDeDocenteDt1(
            @PathVariable Integer idDocente) {
        return service.propuestasDeDocenteDt1(idDocente);
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/dt1/{idDocente}/propuestas/{idPropuesta}/decision")
    public PropuestaComplexivoDto decidirPropuestaDt1(
            @PathVariable Integer idDocente,
            @PathVariable Integer idPropuesta,
            @RequestBody DecisionPropuestaComplexivoRequest req) {
        return service.decidirPropuestaDt1(idDocente, idPropuesta, req);
    }

    // Endpoints legacy propuestas
    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DOCENTE','DOCENTE_TITULADO')")
    @GetMapping("/docente/{idDocente}/propuestas")
    public List<PropuestaComplexivoDto> propuestasDeDocente(
            @PathVariable Integer idDocente) {
        return service.propuestasDeDocente(idDocente);
    }

    @PreAuthorize("hasAnyRole('ADMIN','COORDINADOR','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/docente/{idDocente}/propuestas/{idPropuesta}/decision")
    public PropuestaComplexivoDto decidirPropuesta(
            @PathVariable Integer idDocente,
            @PathVariable Integer idPropuesta,
            @RequestBody DecisionPropuestaComplexivoRequest req) {
        return service.decidirPropuesta(idDocente, idPropuesta, req);
    }

    // ── DT2 — informes ───────────────────────────────────────────────
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @GetMapping("/dt2/{idDocente}/estudiantes")
    public List<EstudianteDeDocenteDto> estudiantesDt2(
            @PathVariable Integer idDocente) {
        return service.estudiantesDeDocenteDt2(idDocente);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @GetMapping("/dt2/{idDocente}/informe/{idComplexivo}")
    public ComplexivoInformeDto getInformeDt2(
            @PathVariable Integer idDocente,
            @PathVariable Integer idComplexivo) {
        return service.getInformeParaDt2(idDocente, idComplexivo);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/dt2/{idDocente}/informe/{idInforme}/aprobar")
    public ComplexivoInformeDto aprobarInforme(
            @PathVariable Integer idDocente,
            @PathVariable Integer idInforme,
            @RequestBody(required = false) Map<String, String> body) {
        return service.revisarInforme(idDocente, idInforme, "APROBADO",
                body != null ? body.get("observaciones") : null);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/dt2/{idDocente}/informe/{idInforme}/rechazar")
    public ComplexivoInformeDto rechazarInforme(
            @PathVariable Integer idDocente,
            @PathVariable Integer idInforme,
            @RequestBody Map<String, String> body) {
        return service.revisarInforme(idDocente, idInforme, "RECHAZADO",
                body.get("observaciones"));
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/dt2/{idDocente}/asesoria/{idComplexivo}")
    public ComplexivoAsesoriaDto registrarAsesoria(
            @PathVariable Integer idDocente,
            @PathVariable Integer idComplexivo,
            @RequestBody RegistrarAsesoriaRequest req) {
        return service.registrarAsesoria(idDocente, idComplexivo, req);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @GetMapping("/dt2/{idDocente}/asesorias/{idComplexivo}")
    public List<ComplexivoAsesoriaDto> listarAsesorias(
            @PathVariable Integer idDocente,
            @PathVariable Integer idComplexivo) {
        return service.listarAsesorias(idDocente, idComplexivo);
    }

    // Endpoints legacy informes
    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @GetMapping("/docente/{idDocente}/estudiantes")
    public List<EstudianteDeDocenteDto> misEstudiantes(
            @PathVariable Integer idDocente) {
        return service.estudiantesDeDocente(idDocente);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @GetMapping("/docente/{idDocente}/informe/{idComplexivo}")
    public ComplexivoInformeDto getInformeDocente(
            @PathVariable Integer idDocente,
            @PathVariable Integer idComplexivo) {
        return service.getInformeParaDocente(idDocente, idComplexivo);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/docente/{idDocente}/informe/{idInforme}/aprobar")
    public ComplexivoInformeDto aprobarInformeLegacy(
            @PathVariable Integer idDocente,
            @PathVariable Integer idInforme,
            @RequestBody(required = false) Map<String, String> body) {
        return service.revisarInforme(idDocente, idInforme, "APROBADO",
                body != null ? body.get("observaciones") : null);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/docente/{idDocente}/informe/{idInforme}/rechazar")
    public ComplexivoInformeDto rechazarInformeLegacy(
            @PathVariable Integer idDocente,
            @PathVariable Integer idInforme,
            @RequestBody Map<String, String> body) {
        return service.revisarInforme(idDocente, idInforme, "RECHAZADO",
                body.get("observaciones"));
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @PostMapping("/docente/{idDocente}/asesoria/{idComplexivo}")
    public ComplexivoAsesoriaDto registrarAsesoriaLegacy(
            @PathVariable Integer idDocente,
            @PathVariable Integer idComplexivo,
            @RequestBody RegistrarAsesoriaRequest req) {
        return service.registrarAsesoria(idDocente, idComplexivo, req);
    }

    @PreAuthorize("hasAnyRole('ADMIN','DOCENTE','DOCENTE_TITULADO')")
    @GetMapping("/docente/{idDocente}/asesorias/{idComplexivo}")
    public List<ComplexivoAsesoriaDto> listarAsesoriasLegacy(
            @PathVariable Integer idDocente,
            @PathVariable Integer idComplexivo) {
        return service.listarAsesorias(idDocente, idComplexivo);
    }
}
