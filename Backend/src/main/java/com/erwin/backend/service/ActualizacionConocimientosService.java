package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.OportunidadDtos.*;
import com.erwin.backend.entities.ActualizacionConocimientos;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.PensumImportado;
import com.erwin.backend.enums.EstadoActualizacionConocimientos;
import com.erwin.backend.enums.EstadoAsignaturaImportada;
import com.erwin.backend.repository.ActualizacionConocimientosRepository;
import com.erwin.backend.repository.EstudianteRepository;
import com.erwin.backend.repository.PensumImportadoRepository;
import com.erwin.backend.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Módulo 13 — actualización de conocimientos previa al reingreso (TAREA 1.2/1.4). */
@Service
@Transactional
public class ActualizacionConocimientosService {

    private static final int MIN_ASIGNATURAS_ADICIONALES = 3;
    private static final int PLAZO_ANIOS_MAXIMO = 5;
    private static final Pattern ANIO_PATTERN = Pattern.compile("(19|20)\\d{2}");

    private final ActualizacionConocimientosRepository repo;
    private final EstudianteRepository estudianteRepo;
    private final PensumImportadoRepository pensumRepo;
    private final AuthorizationService authorizationService;

    public ActualizacionConocimientosService(ActualizacionConocimientosRepository repo,
                                              EstudianteRepository estudianteRepo,
                                              PensumImportadoRepository pensumRepo,
                                              AuthorizationService authorizationService) {
        this.repo = repo;
        this.estudianteRepo = estudianteRepo;
        this.pensumRepo = pensumRepo;
        this.authorizationService = authorizationService;
    }

    @Auditable(entidad = "ActualizacionConocimientos", accion = "SOLICITAR")
    public ActualizacionConocimientosDto solicitar(SolicitarActualizacionRequest req) {
        authorizationService.exigirPropioEstudianteSiAplica(req.idEstudiante());
        Estudiante estudiante = estudianteRepo.findById(req.idEstudiante())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO"));

        List<String> asignaturas = req.asignaturasAdicionales() != null ? req.asignaturasAdicionales() : List.of();
        if (asignaturas.size() < MIN_ASIGNATURAS_ADICIONALES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requieren mínimo " + MIN_ASIGNATURAS_ADICIONALES + " asignaturas adicionales (recibidas: "
                            + asignaturas.size() + ")");
        }
        if (req.periodoOriginal() == null || req.periodoOriginal().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El periodo original es obligatorio");
        }

        validarPlazoCincoAnios(estudiante);

        ActualizacionConocimientos actualizacion = new ActualizacionConocimientos();
        actualizacion.setEstudiante(estudiante);
        actualizacion.setEstado(EstadoActualizacionConocimientos.SOLICITADA);
        actualizacion.setAsignaturasAdicionales(new ArrayList<>(asignaturas));
        actualizacion.setPeriodoOriginal(req.periodoOriginal());
        actualizacion.setPeriodoActualizacion(req.periodoActualizacion());
        actualizacion.setResolucionConsejoDirectivo(req.resolucionConsejoDirectivo());

        return toDto(repo.save(actualizacion));
    }

    @Auditable(entidad = "ActualizacionConocimientos", accion = "RESOLVER")
    public ActualizacionConocimientosDto resolver(Long id, AprobarActualizacionRequest req) {
        ActualizacionConocimientos actualizacion = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SOLICITUD_NO_ENCONTRADA"));

        if (actualizacion.getEstado() != EstadoActualizacionConocimientos.SOLICITADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La solicitud ya fue resuelta (" + actualizacion.getEstado() + ")");
        }

        EstadoActualizacionConocimientos nuevoEstado;
        try {
            nuevoEstado = EstadoActualizacionConocimientos.valueOf(req.estado().trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido: " + req.estado());
        }
        if (nuevoEstado == EstadoActualizacionConocimientos.SOLICITADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado debe ser APROBADA o RECHAZADA");
        }

        actualizacion.setEstado(nuevoEstado);
        if (req.resolucionConsejoDirectivo() != null && !req.resolucionConsejoDirectivo().isBlank()) {
            actualizacion.setResolucionConsejoDirectivo(req.resolucionConsejoDirectivo());
        }
        if (nuevoEstado == EstadoActualizacionConocimientos.APROBADA) {
            actualizacion.setFechaAprobacion(LocalDate.now());
        }

        return toDto(repo.save(actualizacion));
    }

    @Transactional(readOnly = true)
    public List<ActualizacionConocimientosDto> historial(Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        return repo.findByEstudiante_IdEstudianteOrderByFechaSolicitudDesc(idEstudiante)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Art. 12: el estudiante tiene 5 años desde su último periodo académico
     * aprobado para solicitar actualización de conocimientos. Sin registro de
     * periodo aprobado, no se puede verificar el plazo — se rechaza en vez de
     * asumir que cumple.
     */
    private void validarPlazoCincoAnios(Estudiante estudiante) {
        List<PensumImportado> aprobadas = pensumRepo.findByEstudiante_IdEstudianteAndEstado(
                estudiante.getIdEstudiante(), EstadoAsignaturaImportada.APROBADA);

        String ultimoPeriodo = null;
        int ultimoAnio = -1;
        for (PensumImportado p : aprobadas) {
            Integer anio = extraerAnio(p.getPeriodoAcademico());
            if (anio != null && anio > ultimoAnio) {
                ultimoAnio = anio;
                ultimoPeriodo = p.getPeriodoAcademico();
            }
        }

        if (ultimoPeriodo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El estudiante no tiene un periodo académico aprobado registrado; no es posible " +
                            "verificar el plazo de 5 años para solicitar actualización de conocimientos");
        }

        int anioActual = LocalDate.now().getYear();
        if (anioActual - ultimoAnio > PLAZO_ANIOS_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El plazo de " + PLAZO_ANIOS_MAXIMO + " años para solicitar actualización de conocimientos " +
                            "ha vencido. Último periodo aprobado: " + ultimoPeriodo);
        }
    }

    private Integer extraerAnio(String periodoAcademico) {
        if (periodoAcademico == null) return null;
        Matcher m = ANIO_PATTERN.matcher(periodoAcademico);
        int max = -1;
        while (m.find()) {
            max = Math.max(max, Integer.parseInt(m.group()));
        }
        return max == -1 ? null : max;
    }

    private ActualizacionConocimientosDto toDto(ActualizacionConocimientos a) {
        Estudiante est = a.getEstudiante();
        return new ActualizacionConocimientosDto(
                a.getId(),
                est.getIdEstudiante(),
                est.getUsuario() != null ? est.getUsuario().getNombres() + " " + est.getUsuario().getApellidos() : null,
                a.getFechaSolicitud(),
                a.getEstado().name(),
                a.getResolucionConsejoDirectivo(),
                a.getFechaAprobacion(),
                a.getAsignaturasAdicionales(),
                a.getPeriodoOriginal(),
                a.getPeriodoActualizacion()
        );
    }
}
