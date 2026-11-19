package com.erwin.backend.service;

import com.erwin.backend.audit.aspect.Auditable;
import com.erwin.backend.dtos.ExamenComplexivoDtos.*;
import com.erwin.backend.entities.*;
import com.erwin.backend.enums.EstadoExamenComplexivo;
import com.erwin.backend.enums.EstadoEjeTematico;
import com.erwin.backend.enums.EstadoMatricula;
import com.erwin.backend.enums.EstadoPregunta;
import com.erwin.backend.enums.NivelMatricula;
import com.erwin.backend.enums.TipoExamenComplexivo;
import com.erwin.backend.repository.*;
import com.erwin.backend.security.AuthorizationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Motor del Examen Complexivo real: 100 preguntas seleccionadas al azar en
 * el servidor, cronometradas por el servidor (horaFin), con calificación
 * calculada en el servidor. No confundir con {@link InformePracticoService}
 * (tutoría del informe práctico, Art. 20).
 */
@Service
@Transactional
public class ExamenComplexivoService {

    private static final String NOMBRE_MODALIDAD_COMPLEXIVO = "Examen Complexivo";
    private static final int TOTAL_PREGUNTAS_EXAMEN = 100;
    private static final int DURACION_NORMAL_MINUTOS = 180;
    private static final int DURACION_CONADIS_MINUTOS = 240;
    private static final int PORCENTAJE_CONADIS_MINIMO = 60;
    private static final BigDecimal NOTA_APROBACION = BigDecimal.valueOf(7.0);
    private static final BigDecimal VALOR_POR_PREGUNTA = BigDecimal.valueOf(0.1);

    private final ExamenComplexivoAplicacionRepository examenRepo;
    private final ExamenComplexivoRespuestaRepository respuestaRepo;
    private final DelegadoExamenRepository delegadoRepo;
    private final PreguntaComplexivoRepository preguntaRepo;
    private final OpcionPreguntaRepository opcionRepo;
    private final EjeTematicoRepository ejeRepo;
    private final EstudianteRepository estudianteRepo;
    private final DocenteRepository docenteRepo;
    private final DocenteCarreraRepository docenteCarreraRepo;
    private final MatriculaTitulacionRepository matriculaRepo;
    private final PeriodoTitulacionRepository periodoRepo;
    private final OportunidadService oportunidadService;
    private final AuthorizationService authorizationService;

    public ExamenComplexivoService(ExamenComplexivoAplicacionRepository examenRepo,
                                    ExamenComplexivoRespuestaRepository respuestaRepo,
                                    DelegadoExamenRepository delegadoRepo,
                                    PreguntaComplexivoRepository preguntaRepo,
                                    OpcionPreguntaRepository opcionRepo,
                                    EjeTematicoRepository ejeRepo,
                                    EstudianteRepository estudianteRepo,
                                    DocenteRepository docenteRepo,
                                    DocenteCarreraRepository docenteCarreraRepo,
                                    MatriculaTitulacionRepository matriculaRepo,
                                    PeriodoTitulacionRepository periodoRepo,
                                    OportunidadService oportunidadService,
                                    AuthorizationService authorizationService) {
        this.examenRepo = examenRepo;
        this.respuestaRepo = respuestaRepo;
        this.delegadoRepo = delegadoRepo;
        this.preguntaRepo = preguntaRepo;
        this.opcionRepo = opcionRepo;
        this.ejeRepo = ejeRepo;
        this.estudianteRepo = estudianteRepo;
        this.docenteRepo = docenteRepo;
        this.docenteCarreraRepo = docenteCarreraRepo;
        this.matriculaRepo = matriculaRepo;
        this.periodoRepo = periodoRepo;
        this.oportunidadService = oportunidadService;
        this.authorizationService = authorizationService;
    }

    // ═══════════════════════════════════════════════════════════════
    // Programar (TAREA 3.2)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "ExamenComplexivoAplicacion", accion = "PROGRAMAR")
    public ExamenAplicacionResponse programar(ProgramarExamenRequest req) {
        PeriodoTitulacion periodo = getPeriodoActivo();
        validarAvalUnidadRegistro(periodo);
        Estudiante estudiante = estudianteRepo.findById(req.idEstudiante())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ESTUDIANTE_NO_ENCONTRADO"));

        TipoExamenComplexivo tipo = parseTipo(req.tipo());

        MatriculaTitulacion matricula = matriculaRepo
                .findByEstudiante_IdEstudianteAndNivelAndEstado(
                        req.idEstudiante(), NivelMatricula.TITULACION_II, EstadoMatricula.ACTIVA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El estudiante no tiene una matrícula activa en Titulación II"));
        if (matricula.getOpcionTitulacion() == null
                || !NOMBRE_MODALIDAD_COMPLEXIVO.equalsIgnoreCase(matricula.getOpcionTitulacion().getNombre())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El estudiante no tiene la opción Examen Complexivo en su matrícula de Titulación II");
        }

        Carrera carrera = estudiante.getCarrera();
        if (carrera == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ESTUDIANTE_SIN_CARRERA");
        }
        long activas = preguntaRepo.countByCarrera_IdCarreraAndEstado(carrera.getIdCarrera(), EstadoPregunta.ACTIVA);
        if (activas < BancoPreguntasService.MINIMO_PREGUNTAS_ACTIVAS_POR_CARRERA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El banco de preguntas de la carrera solo tiene " + activas + " preguntas activas; "
                            + "se requieren mínimo " + BancoPreguntasService.MINIMO_PREGUNTAS_ACTIVAS_POR_CARRERA);
        }

        if (tipo == TipoExamenComplexivo.SUPLETORIO) {
            ExamenComplexivoAplicacion ultimoFinal = examenRepo
                    .findFirstByEstudiante_IdEstudianteAndPeriodoTitulacion_IdPeriodoAndTipoOrderByIdDesc(
                            req.idEstudiante(), periodo.getIdPeriodo(), TipoExamenComplexivo.FINAL)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "No se puede programar un SUPLETORIO: el estudiante no tiene un examen FINAL en este periodo"));
            boolean reprobado = ultimoFinal.getEstado() == EstadoExamenComplexivo.FINALIZADO
                    && ultimoFinal.getCalificacion() != null
                    && ultimoFinal.getCalificacion().compareTo(NOTA_APROBACION) < 0;
            if (!reprobado) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se puede programar un SUPLETORIO: el examen FINAL no está reprobado (calificación < 7.0)");
            }
        }

        ExamenComplexivoAplicacion examen = new ExamenComplexivoAplicacion();
        examen.setEstudiante(estudiante);
        examen.setPeriodoTitulacion(periodo);
        examen.setTipo(tipo);
        examen.setFechaProgramada(req.fechaProgramada());
        examen.setDuracionMinutos(DURACION_NORMAL_MINUTOS);
        examen.setEstado(EstadoExamenComplexivo.PROGRAMADO);

        return toResponse(examenRepo.save(examen));
    }

    // ═══════════════════════════════════════════════════════════════
    // Registro de ingreso (TAREA 3.5)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "ExamenComplexivoAplicacion", accion = "REGISTRAR_INGRESO")
    public ExamenAplicacionResponse registrarIngreso(Long idExamen, RegistrarIngresoRequest req) {
        ExamenComplexivoAplicacion examen = getExamen(idExamen);
        if (examen.getEstado() != EstadoExamenComplexivo.PROGRAMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede registrar el ingreso de un examen en estado PROGRAMADO");
        }
        examen.setDocumentoIdentidad(req.documentoIdentidad());
        examen.setFirmadoCompromisosEticos(Boolean.TRUE.equals(req.firmadoCompromisosEticos()));
        examen.setHoraIngreso(LocalDateTime.now());
        return toResponse(examenRepo.save(examen));
    }

    // ═══════════════════════════════════════════════════════════════
    // Iniciar (TAREA 3.2) — selección aleatoria + distribución por ejes
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "ExamenComplexivoAplicacion", accion = "INICIAR")
    public IniciarExamenResponse iniciar(Long idExamen) {
        ExamenComplexivoAplicacion examen = getExamen(idExamen);
        if (examen.getEstado() != EstadoExamenComplexivo.PROGRAMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede iniciar un examen en estado PROGRAMADO");
        }
        if (!Boolean.TRUE.equals(examen.getFirmadoCompromisosEticos())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede iniciar el examen: el estudiante no ha firmado los compromisos éticos en el acta de ingreso");
        }

        Estudiante estudiante = examen.getEstudiante();
        Carrera carrera = estudiante.getCarrera();
        List<PreguntaComplexivo> seleccionadas = seleccionarPreguntasAleatorias(carrera.getIdCarrera());
        Collections.shuffle(seleccionadas);

        List<ExamenComplexivoRespuesta> respuestas = new ArrayList<>();
        int orden = 1;
        for (PreguntaComplexivo p : seleccionadas) {
            ExamenComplexivoRespuesta r = new ExamenComplexivoRespuesta();
            r.setExamen(examen);
            r.setPregunta(p);
            r.setOpcionSeleccionada(null);
            r.setOrden(orden++);
            respuestas.add(r);
        }
        respuestaRepo.saveAll(respuestas);

        boolean conadis = Boolean.TRUE.equals(estudiante.getDiscapacidad())
                && estudiante.getPorcentajeDiscapacidad() != null
                && estudiante.getPorcentajeDiscapacidad() >= PORCENTAJE_CONADIS_MINIMO;
        int duracion = conadis ? DURACION_CONADIS_MINUTOS : DURACION_NORMAL_MINUTOS;

        LocalDateTime ahora = LocalDateTime.now();
        examen.setDuracionMinutos(duracion);
        examen.setHoraInicio(ahora);
        examen.setHoraFin(ahora.plusMinutes(duracion));
        examen.setEstado(EstadoExamenComplexivo.EN_CURSO);
        examenRepo.save(examen);

        List<PreguntaExamenDto> preguntasDto = respuestas.stream()
                .map(r -> toPreguntaExamenDto(r.getPregunta()))
                .collect(Collectors.toList());

        return new IniciarExamenResponse(examen.getId(), examen.getHoraInicio(), examen.getHoraFin(),
                examen.getDuracionMinutos(), preguntasDto, java.util.Map.of());
    }

    /**
     * Recupera las 100 preguntas ya asignadas y las respuestas ya registradas
     * de un examen EN_CURSO — usado al recargar la página del estudiante a
     * mitad del examen, sin volver a sortear preguntas ni reiniciar el
     * cronómetro de servidor.
     */
    @Transactional(readOnly = true)
    public IniciarExamenResponse obtenerPreguntasAsignadas(Long idExamen) {
        ExamenComplexivoAplicacion examen = getExamen(idExamen);
        if (examen.getEstado() != EstadoExamenComplexivo.EN_CURSO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El examen no está en curso");
        }
        List<ExamenComplexivoRespuesta> respuestas = respuestaRepo.findByExamen_IdOrderByOrden(idExamen);
        List<PreguntaExamenDto> preguntasDto = respuestas.stream()
                .map(r -> toPreguntaExamenDto(r.getPregunta()))
                .collect(Collectors.toList());
        java.util.Map<Long, Long> seleccionadas = new java.util.HashMap<>();
        for (ExamenComplexivoRespuesta r : respuestas) {
            if (r.getOpcionSeleccionada() != null) {
                seleccionadas.put(r.getPregunta().getId(), r.getOpcionSeleccionada().getId());
            }
        }
        return new IniciarExamenResponse(examen.getId(), examen.getHoraInicio(), examen.getHoraFin(),
                examen.getDuracionMinutos(), preguntasDto, seleccionadas);
    }

    private List<PreguntaComplexivo> seleccionarPreguntasAleatorias(Integer idCarrera) {
        List<EjeTematico> ejes = ejeRepo.findByCarrera_IdCarrera(idCarrera).stream()
                .filter(e -> e.getEstado() == EstadoEjeTematico.ACTIVO)
                .toList();
        if (ejes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La carrera no tiene ejes temáticos activos configurados");
        }

        int cantidadEjes = ejes.size();
        int base = TOTAL_PREGUNTAS_EXAMEN / cantidadEjes;
        int resto = TOTAL_PREGUNTAS_EXAMEN % cantidadEjes;

        List<PreguntaComplexivo> seleccionadas = new ArrayList<>();
        for (int i = 0; i < cantidadEjes; i++) {
            EjeTematico eje = ejes.get(i);
            int cantidad = base + (i < resto ? 1 : 0);
            List<PreguntaComplexivo> disponibles = new ArrayList<>(
                    preguntaRepo.findByEjeTematico_IdAndEstado(eje.getId(), EstadoPregunta.ACTIVA));
            if (disponibles.size() < cantidad) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El eje temático '" + eje.getNombre() + "' no tiene suficientes preguntas activas "
                                + "(tiene " + disponibles.size() + ", se requieren " + cantidad + ")");
            }
            Collections.shuffle(disponibles);
            seleccionadas.addAll(disponibles.subList(0, cantidad));
        }
        return seleccionadas;
    }

    // ═══════════════════════════════════════════════════════════════
    // Responder (TAREA 3.2)
    // ═══════════════════════════════════════════════════════════════
    public void responder(Long idExamen, ResponderPreguntaRequest req) {
        ExamenComplexivoAplicacion examen = getExamen(idExamen);
        if (examen.getEstado() != EstadoExamenComplexivo.EN_CURSO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El examen no está en curso");
        }
        if (LocalDateTime.now().isAfter(examen.getHoraFin())) {
            finalizarInterno(examen);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El tiempo del examen ha vencido; se finalizó automáticamente");
        }

        ExamenComplexivoRespuesta respuesta = respuestaRepo
                .findByExamen_IdAndPregunta_Id(idExamen, req.preguntaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La pregunta no pertenece a este examen"));

        if (req.opcionSeleccionadaId() == null) {
            respuesta.setOpcionSeleccionada(null);
        } else {
            OpcionPregunta opcion = opcionRepo.findById(req.opcionSeleccionadaId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "OPCION_NO_ENCONTRADA"));
            if (!opcion.getPregunta().getId().equals(req.preguntaId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La opción seleccionada no pertenece a la pregunta indicada");
            }
            respuesta.setOpcionSeleccionada(opcion);
        }
        respuestaRepo.save(respuesta);
    }

    // ═══════════════════════════════════════════════════════════════
    // Finalizar (voluntario o por timeout) — TAREA 3.2 / 3.3
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "ExamenComplexivoAplicacion", accion = "FINALIZAR")
    public ResultadoExamenDto finalizar(Long idExamen) {
        ExamenComplexivoAplicacion examen = getExamen(idExamen);
        if (examen.getEstado() != EstadoExamenComplexivo.EN_CURSO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede finalizar un examen en curso");
        }
        finalizarInterno(examen);
        return resultado(idExamen);
    }

    private void finalizarInterno(ExamenComplexivoAplicacion examen) {
        List<ExamenComplexivoRespuesta> respuestas = respuestaRepo.findByExamen_IdOrderByOrden(examen.getId());
        long correctas = respuestas.stream()
                .filter(r -> r.getOpcionSeleccionada() != null && Boolean.TRUE.equals(r.getOpcionSeleccionada().getEsCorrecta()))
                .count();
        BigDecimal calificacion = VALOR_POR_PREGUNTA.multiply(BigDecimal.valueOf(correctas))
                .setScale(2, RoundingMode.HALF_UP);

        examen.setTotalPreguntasCorrectas((int) correctas);
        examen.setCalificacion(calificacion);
        examen.setEstado(EstadoExamenComplexivo.FINALIZADO);
        examen.setHoraFin(LocalDateTime.now());
        examenRepo.save(examen);

        com.erwin.backend.enums.ResultadoOportunidad resultado = calificacion.compareTo(NOTA_APROBACION) >= 0
                ? com.erwin.backend.enums.ResultadoOportunidad.APROBADO
                : com.erwin.backend.enums.ResultadoOportunidad.REPROBADO;
        oportunidadService.registrarResultado(
                examen.getEstudiante(), examen.getPeriodoTitulacion(),
                com.erwin.backend.enums.ModalidadOportunidad.EXAMEN_COMPLEXIVO,
                com.erwin.backend.enums.TipoOportunidad.valueOf(examen.getTipo().name()),
                resultado, calificacion, java.time.LocalDate.now(),
                "Examen Complexivo (" + examen.getTipo() + ") finalizado con " + correctas + " respuestas correctas");
    }

    // ═══════════════════════════════════════════════════════════════
    // Resultado (TAREA 3.2) — nunca revela respuestas correctas
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public ResultadoExamenDto resultado(Long idExamen) {
        ExamenComplexivoAplicacion examen = getExamen(idExamen);
        if (examen.getEstado() != EstadoExamenComplexivo.FINALIZADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El resultado solo está disponible una vez finalizado el examen");
        }
        int total = respuestaRepo.findByExamen_IdOrderByOrden(idExamen).size();
        boolean aprobado = examen.getCalificacion() != null
                && examen.getCalificacion().compareTo(NOTA_APROBACION) >= 0;
        return new ResultadoExamenDto(examen.getId(), examen.getCalificacion(),
                examen.getTotalPreguntasCorrectas(), total, aprobado);
    }

    // ═══════════════════════════════════════════════════════════════
    // Scheduler de timeout (TAREA 3.3) — usado por ExamenComplexivoTimeoutScheduler
    // ═══════════════════════════════════════════════════════════════
    public List<Long> finalizarExamenesVencidos() {
        List<ExamenComplexivoAplicacion> vencidos = examenRepo
                .findByEstadoAndHoraFinBefore(EstadoExamenComplexivo.EN_CURSO, LocalDateTime.now());
        List<Long> finalizados = new ArrayList<>();
        for (ExamenComplexivoAplicacion examen : vencidos) {
            finalizarInterno(examen);
            finalizados.add(examen.getId());
        }
        return finalizados;
    }

    // ═══════════════════════════════════════════════════════════════
    // Fraude académico (TAREA 3.6)
    // ═══════════════════════════════════════════════════════════════
    @Auditable(entidad = "ExamenComplexivoAplicacion", accion = "FRAUDE", capturarArgs = true)
    public ExamenAplicacionResponse registrarFraude(Long idExamen, RegistrarFraudeRequest req) {
        ExamenComplexivoAplicacion examen = getExamen(idExamen);
        if (examen.getEstado() == EstadoExamenComplexivo.ANULADO
                || examen.getEstado() == EstadoExamenComplexivo.FINALIZADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede registrar fraude sobre un examen ya " + examen.getEstado());
        }
        if (req.descripcionIncidente() == null || req.descripcionIncidente().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La descripción del incidente es obligatoria");
        }

        examen.setEstado(EstadoExamenComplexivo.ANULADO);
        examen.setCalificacion(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        examen.setTotalPreguntasCorrectas(0);
        examen.setObservaciones("FRAUDE ACADÉMICO — " + req.descripcionIncidente()
                + (req.evidencia() != null && !req.evidencia().isBlank() ? " | Evidencia: " + req.evidencia() : ""));
        examen.setHoraFin(LocalDateTime.now());
        ExamenComplexivoAplicacion guardado = examenRepo.save(examen);

        oportunidadService.registrarResultado(
                guardado.getEstudiante(), guardado.getPeriodoTitulacion(),
                com.erwin.backend.enums.ModalidadOportunidad.EXAMEN_COMPLEXIVO,
                com.erwin.backend.enums.TipoOportunidad.valueOf(guardado.getTipo().name()),
                com.erwin.backend.enums.ResultadoOportunidad.ANULADO, guardado.getCalificacion(),
                java.time.LocalDate.now(), guardado.getObservaciones());

        return toResponse(guardado);
    }

    // ═══════════════════════════════════════════════════════════════
    // Delegados (TAREA 3.4)
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public List<DelegadoDisponibleDto> delegadosDisponibles(Integer idCarreraEstudiante) {
        return docenteCarreraRepo.findByCarrera_IdCarreraNotAndActivoTrue(idCarreraEstudiante).stream()
                .map(dc -> new DelegadoDisponibleDto(
                        dc.getDocente().getIdDocente(),
                        dc.getDocente().getUsuario() != null
                                ? dc.getDocente().getUsuario().getNombres() + " " + dc.getDocente().getUsuario().getApellidos()
                                : null,
                        dc.getCarrera().getIdCarrera(),
                        dc.getCarrera().getNombre()))
                .distinct()
                .toList();
    }

    @Auditable(entidad = "ExamenComplexivoAplicacion", accion = "ASIGNAR_DELEGADO")
    public ExamenAplicacionResponse asignarDelegado(Long idExamen, AsignarDelegadoRequest req) {
        ExamenComplexivoAplicacion examen = getExamen(idExamen);
        Docente docente = docenteRepo.findById(req.idDocente())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCENTE_NO_ENCONTRADO"));

        Carrera carreraEstudiante = examen.getEstudiante().getCarrera();
        List<DocenteCarrera> vinculaciones = docenteCarreraRepo.findByDocente_IdDocenteAndActivoTrue(req.idDocente());
        DocenteCarrera vinculacionValida = vinculaciones.stream()
                .filter(dc -> carreraEstudiante == null || !dc.getCarrera().getIdCarrera().equals(carreraEstudiante.getIdCarrera()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El docente delegado debe pertenecer a una carrera diferente a la del estudiante"));

        DelegadoExamen delegado = new DelegadoExamen();
        delegado.setExamen(examen);
        delegado.setDocente(docente);
        delegado.setCarreraDocente(vinculacionValida.getCarrera());
        delegadoRepo.save(delegado);

        examen.setDelegadoSupervisor(docente);
        return toResponse(examenRepo.save(examen));
    }

    // ═══════════════════════════════════════════════════════════════
    // Consultas
    // ═══════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    public ExamenAplicacionResponse obtener(Long idExamen) {
        return toResponse(getExamen(idExamen));
    }

    @Transactional(readOnly = true)
    public List<ExamenAplicacionResponse> listarPorEstudiante(Integer idEstudiante) {
        authorizationService.exigirPropioEstudianteSiAplica(idEstudiante);
        PeriodoTitulacion periodo = getPeriodoActivo();
        return examenRepo.findByEstudiante_IdEstudianteAndPeriodoTitulacion_IdPeriodo(idEstudiante, periodo.getIdPeriodo())
                .stream().map(this::toResponse).toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════
    private ExamenComplexivoAplicacion getExamen(Long idExamen) {
        ExamenComplexivoAplicacion examen = examenRepo.findById(idExamen)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "EXAMEN_NO_ENCONTRADO"));
        if (examen.getEstudiante() != null) {
            authorizationService.exigirPropioEstudianteSiAplica(examen.getEstudiante().getIdEstudiante());
        }
        return examen;
    }

    private PeriodoTitulacion getPeriodoActivo() {
        return periodoRepo.findFirstByActivoTrueOrderByIdPeriodoDesc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_HAY_PERIODO_ACTIVO"));
    }

    /** Módulo 18, TAREA 3.2: las actividades del calendario solo se ejecutan con aval de Unidad de Registro. */
    private void validarAvalUnidadRegistro(PeriodoTitulacion periodo) {
        if (!Boolean.TRUE.equals(periodo.getAvalUnidadRegistro())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede programar: el periodo " + periodo.getDescripcion()
                            + " todavía no tiene el aval de la Unidad de Registro");
        }
    }

    private TipoExamenComplexivo parseTipo(String tipo) {
        try {
            return TipoExamenComplexivo.valueOf(tipo.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de examen inválido: " + tipo);
        }
    }

    private PreguntaExamenDto toPreguntaExamenDto(PreguntaComplexivo p) {
        List<OpcionExamenDto> opciones = p.getOpciones().stream()
                .sorted((a, b) -> a.getOrden().compareTo(b.getOrden()))
                .map(o -> new OpcionExamenDto(o.getId(), o.getTexto(), o.getOrden()))
                .toList();
        return new PreguntaExamenDto(p.getId(), p.getEnunciado(), opciones);
    }

    private ExamenAplicacionResponse toResponse(ExamenComplexivoAplicacion e) {
        Estudiante est = e.getEstudiante();
        Carrera carrera = est != null ? est.getCarrera() : null;
        return new ExamenAplicacionResponse(
                e.getId(),
                est != null ? est.getIdEstudiante() : null,
                est != null && est.getUsuario() != null ? est.getUsuario().getNombres() + " " + est.getUsuario().getApellidos() : null,
                carrera != null ? carrera.getIdCarrera() : null,
                carrera != null ? carrera.getNombre() : null,
                e.getPeriodoTitulacion() != null ? e.getPeriodoTitulacion().getIdPeriodo() : null,
                e.getTipo() != null ? e.getTipo().name() : null,
                e.getFechaProgramada(),
                e.getHoraInicio(),
                e.getHoraFin(),
                e.getDuracionMinutos(),
                e.getEstado() != null ? e.getEstado().name() : null,
                e.getCalificacion(),
                e.getTotalPreguntasCorrectas(),
                e.getDelegadoSupervisor() != null ? e.getDelegadoSupervisor().getIdDocente() : null,
                e.getDelegadoSupervisor() != null && e.getDelegadoSupervisor().getUsuario() != null
                        ? e.getDelegadoSupervisor().getUsuario().getNombres() + " " + e.getDelegadoSupervisor().getUsuario().getApellidos()
                        : null,
                e.getObservaciones()
        );
    }
}
