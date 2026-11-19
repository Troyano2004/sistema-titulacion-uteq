package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.BancoPreguntasDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.DificultadPregunta;
import com.erwin.backend.enums.EstadoPregunta;
import com.erwin.backend.repository.*;
import com.erwin.backend.security.AuthorizationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Banco de preguntas del Examen Complexivo (Módulo 12/TAREA 2). El acceso
 * queda restringido a docentes/coordinadores en el controller — los
 * estudiantes nunca tienen un endpoint que exponga estas preguntas.
 */
@Service
@Transactional
public class BancoPreguntasService {

    public static final int MINIMO_PREGUNTAS_ACTIVAS_POR_CARRERA = 300;
    private static final int SEMANA_LIMITE_ACTIVACION = 10;
    private static final int MIN_OPCIONES = 2;
    private static final int MAX_OPCIONES = 5;

    private final PreguntaComplexivoRepository preguntaRepo;
    private final EjeTematicoRepository ejeRepo;
    private final CarreraRepository carreraRepo;
    private final DocenteRepository docenteRepo;
    private final PeriodoTitulacionRepository periodoRepo;
    private final AuthorizationService authorizationService;

    public BancoPreguntasService(PreguntaComplexivoRepository preguntaRepo,
                                  EjeTematicoRepository ejeRepo,
                                  CarreraRepository carreraRepo,
                                  DocenteRepository docenteRepo,
                                  PeriodoTitulacionRepository periodoRepo,
                                  AuthorizationService authorizationService) {
        this.preguntaRepo = preguntaRepo;
        this.ejeRepo = ejeRepo;
        this.carreraRepo = carreraRepo;
        this.docenteRepo = docenteRepo;
        this.periodoRepo = periodoRepo;
        this.authorizationService = authorizationService;
    }

    @Auditable(entidad = "PreguntaComplexivo", accion = "CREATE")
    public PreguntaComplexivoResponse crear(PreguntaComplexivoRequest req) {
        authorizationService.exigirPropiaCarreraSiCoordinador(req.idCarrera());
        if (req.enunciado() == null || req.enunciado().isBlank()) {
            throw new IllegalArgumentException("El enunciado es obligatorio");
        }
        if (req.idDocenteCreador() == null) {
            throw new IllegalArgumentException("El docente creador es obligatorio");
        }

        EjeTematico eje = ejeRepo.findById(req.idEjeTematico())
                .orElseThrow(() -> new EntityNotFoundException("Eje temático no encontrado: " + req.idEjeTematico()));
        Carrera carrera = carreraRepo.findById(req.idCarrera())
                .orElseThrow(() -> new EntityNotFoundException("Carrera no encontrada: " + req.idCarrera()));
        Docente creador = docenteRepo.findById(req.idDocenteCreador())
                .orElseThrow(() -> new EntityNotFoundException("Docente no encontrado: " + req.idDocenteCreador()));

        DificultadPregunta dificultad = parseDificultad(req.dificultad());
        EstadoPregunta estado = parseEstado(req.estado(), EstadoPregunta.EN_REVISION);
        if (estado == EstadoPregunta.ACTIVA) {
            validarPlazoActivacion(req.periodoAcademico());
        }

        PreguntaComplexivo pregunta = new PreguntaComplexivo();
        pregunta.setEjeTematico(eje);
        pregunta.setCarrera(carrera);
        pregunta.setEnunciado(req.enunciado());
        pregunta.setDificultad(dificultad);
        pregunta.setEstado(estado);
        pregunta.setCreadaPor(creador);
        pregunta.setPeriodoAcademico(req.periodoAcademico());
        pregunta.setOpciones(construirOpciones(pregunta, req.opciones()));

        return convertirADto(preguntaRepo.save(pregunta));
    }

    @Auditable(entidad = "PreguntaComplexivo", accion = "UPDATE")
    public PreguntaComplexivoResponse actualizar(Long id, PreguntaComplexivoRequest req) {
        PreguntaComplexivo pregunta = preguntaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pregunta no encontrada: " + id));
        if (pregunta.getCarrera() != null) {
            authorizationService.exigirPropiaCarreraSiCoordinador(pregunta.getCarrera().getIdCarrera());
        }
        if (req.idCarrera() != null) {
            authorizationService.exigirPropiaCarreraSiCoordinador(req.idCarrera());
        }

        if (req.idEjeTematico() != null) {
            pregunta.setEjeTematico(ejeRepo.findById(req.idEjeTematico())
                    .orElseThrow(() -> new EntityNotFoundException("Eje temático no encontrado: " + req.idEjeTematico())));
        }
        if (req.idCarrera() != null) {
            pregunta.setCarrera(carreraRepo.findById(req.idCarrera())
                    .orElseThrow(() -> new EntityNotFoundException("Carrera no encontrada: " + req.idCarrera())));
        }
        if (req.enunciado() != null && !req.enunciado().isBlank()) {
            pregunta.setEnunciado(req.enunciado());
        }
        if (req.dificultad() != null) {
            pregunta.setDificultad(parseDificultad(req.dificultad()));
        }
        if (req.periodoAcademico() != null) {
            pregunta.setPeriodoAcademico(req.periodoAcademico());
        }
        if (req.opciones() != null && !req.opciones().isEmpty()) {
            pregunta.getOpciones().clear();
            pregunta.getOpciones().addAll(construirOpciones(pregunta, req.opciones()));
        }

        EstadoPregunta nuevoEstado = parseEstado(req.estado(), pregunta.getEstado());
        boolean seActiva = nuevoEstado == EstadoPregunta.ACTIVA && pregunta.getEstado() != EstadoPregunta.ACTIVA;
        if (seActiva) {
            validarLaPreguntaEsValida(pregunta);
            validarPlazoActivacion(pregunta.getPeriodoAcademico());
        }
        pregunta.setEstado(nuevoEstado);

        return convertirADto(preguntaRepo.save(pregunta));
    }

    @Auditable(entidad = "PreguntaComplexivo", accion = "DESACTIVAR")
    public PreguntaComplexivoResponse desactivar(Long id) {
        PreguntaComplexivo pregunta = preguntaRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pregunta no encontrada: " + id));
        if (pregunta.getCarrera() != null) {
            authorizationService.exigirPropiaCarreraSiCoordinador(pregunta.getCarrera().getIdCarrera());
        }
        pregunta.setEstado(EstadoPregunta.INACTIVA);
        return convertirADto(preguntaRepo.save(pregunta));
    }

    @Transactional(readOnly = true)
    public List<PreguntaComplexivoResponse> listar(Integer idCarrera, Long idEjeTematico, String estadoStr) {
        authorizationService.exigirPropiaCarreraSiCoordinador(idCarrera);
        EstadoPregunta estado = (estadoStr == null || estadoStr.isBlank()) ? null : parseEstado(estadoStr, null);
        List<PreguntaComplexivo> preguntas;
        if (idEjeTematico != null && estado != null) {
            preguntas = preguntaRepo.findByCarrera_IdCarreraAndEjeTematico_IdAndEstado(idCarrera, idEjeTematico, estado);
        } else if (idEjeTematico != null) {
            preguntas = preguntaRepo.findByCarrera_IdCarreraAndEjeTematico_Id(idCarrera, idEjeTematico);
        } else if (estado != null) {
            preguntas = preguntaRepo.findByCarrera_IdCarreraAndEstado(idCarrera, estado);
        } else {
            preguntas = preguntaRepo.findByCarrera_IdCarrera(idCarrera);
        }
        return preguntas.stream().map(this::convertirADto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EstadisticasBancoDto estadisticas(Integer idCarrera) {
        authorizationService.exigirPropiaCarreraSiCoordinador(idCarrera);
        long total = preguntaRepo.countByCarrera_IdCarreraAndEstado(idCarrera, EstadoPregunta.ACTIVA);
        List<EjeTematico> ejes = ejeRepo.findByCarrera_IdCarrera(idCarrera);
        Map<String, Long> porEje = new LinkedHashMap<>();
        for (EjeTematico eje : ejes) {
            porEje.put(eje.getNombre(), preguntaRepo.countByCarrera_IdCarreraAndEjeTematico_IdAndEstado(
                    idCarrera, eje.getId(), EstadoPregunta.ACTIVA));
        }
        return new EstadisticasBancoDto(idCarrera, total, porEje,
                MINIMO_PREGUNTAS_ACTIVAS_POR_CARRERA, total >= MINIMO_PREGUNTAS_ACTIVAS_POR_CARRERA);
    }

    // ═══════════════════════════════════════════════════════════════
    // Validaciones
    // ═══════════════════════════════════════════════════════════════
    private List<OpcionPregunta> construirOpciones(PreguntaComplexivo pregunta, List<OpcionRequest> reqOpciones) {
        if (reqOpciones == null || reqOpciones.size() < MIN_OPCIONES || reqOpciones.size() > MAX_OPCIONES) {
            throw new IllegalArgumentException(
                    "Cada pregunta debe tener entre " + MIN_OPCIONES + " y " + MAX_OPCIONES + " opciones");
        }
        long correctas = reqOpciones.stream().filter(o -> Boolean.TRUE.equals(o.esCorrecta())).count();
        if (correctas != 1) {
            throw new IllegalArgumentException(
                    "Cada pregunta debe tener exactamente 1 opción correcta (recibidas: " + correctas + ")");
        }
        List<OpcionPregunta> opciones = new ArrayList<>();
        int orden = 1;
        for (OpcionRequest o : reqOpciones) {
            if (o.texto() == null || o.texto().isBlank()) {
                throw new IllegalArgumentException("El texto de la opción es obligatorio");
            }
            OpcionPregunta opcion = new OpcionPregunta();
            opcion.setPregunta(pregunta);
            opcion.setTexto(o.texto());
            opcion.setEsCorrecta(Boolean.TRUE.equals(o.esCorrecta()));
            opcion.setOrden(o.orden() != null ? o.orden() : orden);
            opciones.add(opcion);
            orden++;
        }
        return opciones;
    }

    private void validarLaPreguntaEsValida(PreguntaComplexivo pregunta) {
        if (pregunta.getOpciones() == null
                || pregunta.getOpciones().size() < MIN_OPCIONES
                || pregunta.getOpciones().size() > MAX_OPCIONES) {
            throw new IllegalStateException(
                    "No se puede activar: la pregunta debe tener entre " + MIN_OPCIONES + " y " + MAX_OPCIONES + " opciones");
        }
        long correctas = pregunta.getOpciones().stream().filter(o -> Boolean.TRUE.equals(o.getEsCorrecta())).count();
        if (correctas != 1) {
            throw new IllegalStateException(
                    "No se puede activar: la pregunta debe tener exactamente 1 opción correcta");
        }
    }

    private void validarPlazoActivacion(String periodoAcademico) {
        PeriodoTitulacion periodo = periodoAcademico == null
                ? null : periodoRepo.findByDescripcion(periodoAcademico).orElse(null);
        if (periodo == null || periodo.getFechaInicio() == null) return;

        int semana = com.erwin.backend.util.CalendarioAcademicoUtil.semanaActual(periodo.getFechaInicio(), LocalDate.now());
        if (semana > SEMANA_LIMITE_ACTIVACION) {
            throw new IllegalStateException(
                    "No se puede activar la pregunta: la fecha límite de entrega del banco de preguntas "
                            + "es la semana " + SEMANA_LIMITE_ACTIVACION + " del periodo (semana actual: " + semana + ")");
        }
    }

    private DificultadPregunta parseDificultad(String dificultad) {
        if (dificultad == null || dificultad.isBlank()) {
            throw new IllegalArgumentException("La dificultad es obligatoria");
        }
        try {
            return DificultadPregunta.valueOf(dificultad.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Dificultad inválida: " + dificultad);
        }
    }

    private EstadoPregunta parseEstado(String estado, EstadoPregunta porDefecto) {
        if (estado == null || estado.isBlank()) return porDefecto;
        try {
            return EstadoPregunta.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Estado de pregunta inválido: " + estado);
        }
    }

    private PreguntaComplexivoResponse convertirADto(PreguntaComplexivo p) {
        List<OpcionResponse> opciones = p.getOpciones().stream()
                .map(o -> new OpcionResponse(o.getId(), o.getTexto(), o.getEsCorrecta(), o.getOrden()))
                .collect(Collectors.toList());
        return new PreguntaComplexivoResponse(
                p.getId(),
                p.getEjeTematico() != null ? p.getEjeTematico().getId() : null,
                p.getEjeTematico() != null ? p.getEjeTematico().getNombre() : null,
                p.getCarrera() != null ? p.getCarrera().getIdCarrera() : null,
                p.getEnunciado(),
                p.getDificultad() != null ? p.getDificultad().name() : null,
                p.getEstado() != null ? p.getEstado().name() : null,
                p.getCreadaPor() != null ? p.getCreadaPor().getIdDocente() : null,
                p.getCreadaPor() != null && p.getCreadaPor().getUsuario() != null
                        ? (p.getCreadaPor().getUsuario().getNombres() + " " + p.getCreadaPor().getUsuario().getApellidos())
                        : null,
                p.getFechaCreacion(),
                p.getPeriodoAcademico(),
                opciones
        );
    }
}
