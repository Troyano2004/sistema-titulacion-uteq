package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.CalendarioDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Módulo 18 — vista unificada del calendario de titulación (TAREA 3). */
@Service
@Transactional(readOnly = true)
public class CalendarioTitulacionService {

    private static final int SEMANAS_LIMITE_PROPUESTA_TIC = 4;

    private final PeriodoTitulacionRepository periodoRepo;
    private final ExamenComplexivoAplicacionRepository examenRepo;
    private final SustentacionRepository sustentacionRepo;
    private final TribunalProyectoRepository tribunalRepo;

    public CalendarioTitulacionService(PeriodoTitulacionRepository periodoRepo,
                                        ExamenComplexivoAplicacionRepository examenRepo,
                                        SustentacionRepository sustentacionRepo,
                                        TribunalProyectoRepository tribunalRepo) {
        this.periodoRepo = periodoRepo;
        this.examenRepo = examenRepo;
        this.sustentacionRepo = sustentacionRepo;
        this.tribunalRepo = tribunalRepo;
    }

    public List<EventoCalendarioDto> obtenerEventos(String periodoDescripcion, Integer idFacultad) {
        PeriodoTitulacion periodo = resolverPeriodo(periodoDescripcion);
        List<EventoCalendarioDto> eventos = new ArrayList<>();

        agregarExamenesComplexivo(eventos, periodo, idFacultad);
        agregarSustentaciones(eventos, periodo, idFacultad, "PREDEFENSA", "PREDEFENSA");
        agregarSustentaciones(eventos, periodo, idFacultad, "DEFENSA_FINAL", "SUSTENTACION");
        agregarPlazos(eventos, periodo, idFacultad);

        eventos.sort(Comparator.comparing(EventoCalendarioDto::fecha,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return eventos;
    }

    private void agregarExamenesComplexivo(List<EventoCalendarioDto> eventos, PeriodoTitulacion periodo, Integer idFacultad) {
        for (ExamenComplexivoAplicacion e : examenRepo.findByPeriodoTitulacion_IdPeriodo(periodo.getIdPeriodo())) {
            Estudiante est = e.getEstudiante();
            Carrera carrera = est != null ? est.getCarrera() : null;
            if (!coincideFacultad(carrera, idFacultad)) continue;

            String nombreEstudiante = est != null && est.getUsuario() != null
                    ? est.getUsuario().getNombres() + " " + est.getUsuario().getApellidos() : null;
            String nombreDelegado = e.getDelegadoSupervisor() != null && e.getDelegadoSupervisor().getUsuario() != null
                    ? e.getDelegadoSupervisor().getUsuario().getNombres() + " " + e.getDelegadoSupervisor().getUsuario().getApellidos()
                    : null;

            eventos.add(new EventoCalendarioDto(
                    "EXAMEN_COMPLEXIVO_" + e.getTipo(),
                    e.getFechaProgramada(),
                    e.getHoraInicio() != null ? e.getHoraInicio().toLocalTime() : null,
                    null,
                    "Examen Complexivo (" + e.getTipo() + ") — " + nombreEstudiante,
                    carrera != null ? carrera.getIdCarrera() : null,
                    carrera != null ? carrera.getNombre() : null,
                    nombreEstudiante != null ? List.of(nombreEstudiante) : List.of(),
                    nombreDelegado != null ? List.of(nombreDelegado) : List.of()
            ));
        }
    }

    private void agregarSustentaciones(List<EventoCalendarioDto> eventos, PeriodoTitulacion periodo,
                                        Integer idFacultad, String tipoInterno, String tipoEvento) {
        List<Sustentacion> lista = sustentacionRepo.findByTipoAndFechaBetween(
                tipoInterno, periodo.getFechaInicio(), periodo.getFechaFin());

        for (Sustentacion s : lista) {
            ProyectoTitulacion proyecto = s.getProyecto();
            if (proyecto == null || proyecto.getPropuesta() == null) continue;
            Carrera carrera = proyecto.getPropuesta().getCarrera();
            if (!coincideFacultad(carrera, idFacultad)) continue;

            Estudiante est = proyecto.getPropuesta().getEstudiante();
            String nombreEstudiante = est != null && est.getUsuario() != null
                    ? est.getUsuario().getNombres() + " " + est.getUsuario().getApellidos() : null;

            List<String> docentes = new ArrayList<>();
            if (proyecto.getDirector() != null && proyecto.getDirector().getUsuario() != null) {
                docentes.add(proyecto.getDirector().getUsuario().getNombres() + " " + proyecto.getDirector().getUsuario().getApellidos());
            }
            for (TribunalProyecto t : tribunalRepo.findByProyecto_IdProyecto(proyecto.getIdProyecto())) {
                if (t.getDocente() != null && t.getDocente().getUsuario() != null) {
                    docentes.add(t.getDocente().getUsuario().getNombres() + " " + t.getDocente().getUsuario().getApellidos());
                }
            }

            eventos.add(new EventoCalendarioDto(
                    tipoEvento,
                    s.getFecha(),
                    s.getHora(),
                    s.getLugar(),
                    tipoEvento + " — " + proyecto.getTitulo(),
                    carrera != null ? carrera.getIdCarrera() : null,
                    carrera != null ? carrera.getNombre() : null,
                    nombreEstudiante != null ? List.of(nombreEstudiante) : List.of(),
                    docentes
            ));
        }
    }

    private void agregarPlazos(List<EventoCalendarioDto> eventos, PeriodoTitulacion periodo, Integer idFacultad) {
        if (idFacultad != null) return; // los plazos institucionales aplican a todas las facultades

        if (periodo.getFechaInicio() != null) {
            eventos.add(plazo("PLAZO_PROPUESTA_TIC",
                    periodo.getFechaInicio().plusWeeks(SEMANAS_LIMITE_PROPUESTA_TIC),
                    "Fecha límite para presentar la propuesta del TIC (semana " + SEMANAS_LIMITE_PROPUESTA_TIC + ")"));
        }
        if (periodo.getFechaFinCorte1() != null) {
            eventos.add(plazo("PLAZO_CIERRE_CORTE1", periodo.getFechaFinCorte1(), "Fecha límite de cierre del corte evaluativo 1"));
        }
        if (periodo.getFechaFinCorte2() != null) {
            eventos.add(plazo("PLAZO_CIERRE_CORTE2", periodo.getFechaFinCorte2(), "Fecha límite de cierre del corte evaluativo 2"));
        }
    }

    private EventoCalendarioDto plazo(String tipo, LocalDate fecha, String descripcion) {
        return new EventoCalendarioDto(tipo, fecha, null, null, descripcion, null, null, List.of(), List.of());
    }

    private boolean coincideFacultad(Carrera carrera, Integer idFacultad) {
        if (idFacultad == null) return true;
        return carrera != null && carrera.getFacultad() != null
                && idFacultad.equals(carrera.getFacultad().getIdFacultad());
    }

    private PeriodoTitulacion resolverPeriodo(String periodoDescripcion) {
        if (periodoDescripcion != null && !periodoDescripcion.isBlank()) {
            return periodoRepo.findByDescripcion(periodoDescripcion)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PERIODO_NO_ENCONTRADO"));
        }
        return periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));
    }

    // ═══════════════════════════════════════════════════════════════
    // Aval de Unidad de Registro (TAREA 3.2)
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    @Auditable(entidad = "PeriodoTitulacion", accion = "AVAL_UNIDAD_REGISTRO")
    public PeriodoAvalDto darAval(Integer idPeriodo, AvalUnidadRegistroRequest req) {
        PeriodoTitulacion periodo = periodoRepo.findById(idPeriodo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PERIODO_NO_ENCONTRADO"));
        if (req.avaladoPor() == null || req.avaladoPor().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar quién otorga el aval");
        }
        periodo.setAvalUnidadRegistro(true);
        periodo.setFechaAval(LocalDate.now());
        periodo.setAvaladoPor(req.avaladoPor());
        periodoRepo.save(periodo);
        return toAvalDto(periodo);
    }

    public PeriodoAvalDto obtenerAval(Integer idPeriodo) {
        PeriodoTitulacion periodo = periodoRepo.findById(idPeriodo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PERIODO_NO_ENCONTRADO"));
        return toAvalDto(periodo);
    }

    private PeriodoAvalDto toAvalDto(PeriodoTitulacion p) {
        return new PeriodoAvalDto(p.getIdPeriodo(), p.getDescripcion(), p.getAvalUnidadRegistro(), p.getFechaAval(), p.getAvaladoPor());
    }
}
