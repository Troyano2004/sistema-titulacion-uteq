package com.erwin.backend.service;

import com.erwin.backend.dtos.reporte.DashboardReportesDto;
import com.erwin.backend.dtos.reporte.ReporteExpedientesDto;
import com.erwin.backend.dtos.reporte.ResultadosFacultadDto;
import com.erwin.backend.dtos.reporte.TrazabilidadEstudianteDto;
import com.erwin.backend.entities.*;
import com.erwin.backend.repository.*;
import com.erwin.backend.security.AuthorizationService;
import com.erwin.backend.util.CalendarioAcademicoUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reportes institucionales (Módulo de auditoría y reportes, TAREA 2). Todas las
 * agregaciones estadísticas se resuelven con consultas JPQL GROUP BY en los
 * repositorios — no se usa findAll() + filtrado en memoria (ver REGLAS). La
 * trazabilidad de un único estudiante sí hace varias consultas puntuales
 * (una por entidad del flujo), cada una acotada por idEstudiante/idProyecto:
 * es la forma natural de construir una línea de tiempo, no un barrido masivo.
 */
@Service
public class ReportesInstitucionalesService {

    private static final BigDecimal NOTA_APROBACION = new BigDecimal("7.00");
    private static final int PLAZO_ART40_DIAS_HABILES = 8;

    private final FacultadRepository facultadRepo;
    private final PeriodoTitulacionRepository periodoRepo;
    private final EstudianteRepository estudianteRepo;
    private final ProyectoTitulacionRepository proyectoRepo;
    private final ExamenComplexivoAplicacionRepository examenRepo;
    private final MatriculaTitulacionRepository matriculaRepo;
    private final ActaGradoRepository actaGradoRepo;
    private final ExpedienteEstudianteRepository expedienteRepo;
    private final DocumentoExpedienteRepository documentoExpedienteRepo;
    private final EleccionTitulacionRepository eleccionRepo;
    private final PropuestaTitulacionRepository propuestaRepo;
    private final AnteproyectoTitulacionRepository anteproyectoRepo;
    private final TutoriaAnteproyectoRepository tutoriaRepo;
    private final BitacoraAsignacionRepository bitacoraRepo;
    private final AsesoriaDirectorRepository asesoriaRepo;
    private final SustentacionRepository sustentacionRepo;
    private final TituloProfesionalRepository tituloRepo;
    private final CarreraRepository carreraRepo;
    private final AuthorizationService authorizationService;

    public ReportesInstitucionalesService(FacultadRepository facultadRepo, PeriodoTitulacionRepository periodoRepo,
                                          EstudianteRepository estudianteRepo, ProyectoTitulacionRepository proyectoRepo,
                                          ExamenComplexivoAplicacionRepository examenRepo, MatriculaTitulacionRepository matriculaRepo,
                                          ActaGradoRepository actaGradoRepo, ExpedienteEstudianteRepository expedienteRepo,
                                          DocumentoExpedienteRepository documentoExpedienteRepo, EleccionTitulacionRepository eleccionRepo,
                                          PropuestaTitulacionRepository propuestaRepo, AnteproyectoTitulacionRepository anteproyectoRepo,
                                          TutoriaAnteproyectoRepository tutoriaRepo, BitacoraAsignacionRepository bitacoraRepo,
                                          AsesoriaDirectorRepository asesoriaRepo, SustentacionRepository sustentacionRepo,
                                          TituloProfesionalRepository tituloRepo, CarreraRepository carreraRepo,
                                          AuthorizationService authorizationService) {
        this.facultadRepo = facultadRepo;
        this.periodoRepo = periodoRepo;
        this.estudianteRepo = estudianteRepo;
        this.proyectoRepo = proyectoRepo;
        this.examenRepo = examenRepo;
        this.matriculaRepo = matriculaRepo;
        this.actaGradoRepo = actaGradoRepo;
        this.expedienteRepo = expedienteRepo;
        this.documentoExpedienteRepo = documentoExpedienteRepo;
        this.eleccionRepo = eleccionRepo;
        this.propuestaRepo = propuestaRepo;
        this.anteproyectoRepo = anteproyectoRepo;
        this.tutoriaRepo = tutoriaRepo;
        this.bitacoraRepo = bitacoraRepo;
        this.asesoriaRepo = asesoriaRepo;
        this.sustentacionRepo = sustentacionRepo;
        this.tituloRepo = tituloRepo;
        this.carreraRepo = carreraRepo;
        this.authorizationService = authorizationService;
    }

    // ── 2.1 RESULTADOS POR FACULTAD ─────────────────────────────────────────

    public ResultadosFacultadDto getResultadosFacultad(Integer idFacultad, String periodo) {
        authorizationService.exigirPropiaFacultadSiCoordinador(idFacultad);
        Facultad facultad = facultadRepo.findById(idFacultad)
            .orElseThrow(() -> new RuntimeException("Facultad no encontrada"));
        periodoRepo.findByDescripcion(periodo)
            .orElseThrow(() -> new RuntimeException("Periodo no encontrado: " + periodo));

        List<Object[]> filasTic = proyectoRepo.resultadosPorCarreraYModalidad(idFacultad, periodo);
        List<Object[]> filasComplexivo = examenRepo.resultadosPorCarrera(idFacultad, periodo, NOTA_APROBACION);

        java.util.Map<Integer, ResultadosFacultadDto.CarreraResultadoDto> porCarrera = new java.util.LinkedHashMap<>();
        java.util.Map<Integer, List<ResultadosFacultadDto.ModalidadResultadoDto>> modalidadesPorCarrera = new java.util.LinkedHashMap<>();

        for (Object[] fila : filasTic) {
            Integer idCarrera = (Integer) fila[0];
            String nombreCarrera = (String) fila[1];
            String modalidad = (String) fila[2];
            long total = numOrZero(fila[3]);
            long aprobados = numOrZero(fila[4]);
            long reprobados = numOrZero(fila[5]);
            BigDecimal promedio = fila[6] != null ? redondear((java.math.BigDecimal) fila[6]) : null;

            modalidadesPorCarrera.computeIfAbsent(idCarrera, k -> new ArrayList<>())
                .add(ResultadosFacultadDto.ModalidadResultadoDto.builder()
                    .modalidad(modalidad).total((int) total).aprobados((int) aprobados)
                    .reprobados((int) reprobados).promedioCalificacion(promedio).build());
            porCarrera.computeIfAbsent(idCarrera, k -> ResultadosFacultadDto.CarreraResultadoDto.builder()
                .carrera(nombreCarrera).totalEstudiantes(0).build());
        }

        for (Object[] fila : filasComplexivo) {
            Integer idCarrera = (Integer) fila[0];
            String nombreCarrera = (String) fila[1];
            long total = numOrZero(fila[2]);
            long aprobados = numOrZero(fila[3]);
            long reprobados = numOrZero(fila[4]);
            BigDecimal promedio = fila[5] != null ? redondear((java.math.BigDecimal) fila[5]) : null;

            modalidadesPorCarrera.computeIfAbsent(idCarrera, k -> new ArrayList<>())
                .add(ResultadosFacultadDto.ModalidadResultadoDto.builder()
                    .modalidad("EXAMEN_COMPLEXIVO").total((int) total).aprobados((int) aprobados)
                    .reprobados((int) reprobados).promedioCalificacion(promedio).build());
            porCarrera.computeIfAbsent(idCarrera, k -> ResultadosFacultadDto.CarreraResultadoDto.builder()
                .carrera(nombreCarrera).totalEstudiantes(0).build());
        }

        int totalEstudiantes = 0, totalAprobados = 0, totalReprobados = 0;
        List<ResultadosFacultadDto.CarreraResultadoDto> carreras = new ArrayList<>();
        for (var entry : porCarrera.entrySet()) {
            List<ResultadosFacultadDto.ModalidadResultadoDto> mods = modalidadesPorCarrera.get(entry.getKey());
            int totalCarrera = mods.stream().mapToInt(ResultadosFacultadDto.ModalidadResultadoDto::getTotal).sum();
            int aprobCarrera = mods.stream().mapToInt(ResultadosFacultadDto.ModalidadResultadoDto::getAprobados).sum();
            totalEstudiantes += totalCarrera;
            totalAprobados += aprobCarrera;
            totalReprobados += mods.stream().mapToInt(ResultadosFacultadDto.ModalidadResultadoDto::getReprobados).sum();
            entry.getValue().setTotalEstudiantes(totalCarrera);
            entry.getValue().setPorModalidad(mods);
            entry.getValue().setTasaAprobacion(totalCarrera > 0 ? (aprobCarrera * 100.0 / totalCarrera) : 0.0);
            carreras.add(entry.getValue());
        }

        LocalDateTime ultimoExamen = examenRepo.ultimaFechaExamenFinalizado(idFacultad, periodo);
        LocalDate fechaLimite = ultimoExamen != null
            ? CalendarioAcademicoUtil.sumarDiasHabiles(ultimoExamen.toLocalDate(), PLAZO_ART40_DIAS_HABILES)
            : null;

        return ResultadosFacultadDto.builder()
            .facultad(facultad.getNombre())
            .periodo(periodo)
            .fechaGeneracion(LocalDate.now())
            .fechaLimitePlazoArt40(fechaLimite)
            .dentroDePlazo(fechaLimite == null || !LocalDate.now().isAfter(fechaLimite))
            .totalEstudiantes(totalEstudiantes)
            .totalAprobados(totalAprobados)
            .totalReprobados(totalReprobados)
            .tasaAprobacionGeneral(totalEstudiantes > 0 ? (totalAprobados * 100.0 / totalEstudiantes) : 0.0)
            .carreras(carreras)
            .build();
    }

    private static BigDecimal redondear(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    /** SUM(...) sin filas coincidentes devuelve NULL (no 0) en SQL — se trata como 0. */
    private static long numOrZero(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    // ── 2.2 TRAZABILIDAD ────────────────────────────────────────────────────

    public TrazabilidadEstudianteDto getTrazabilidad(Integer idEstudiante) {
        Estudiante est = estudianteRepo.findById(idEstudiante)
            .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        authorizationService.exigirPropiaCarreraSiCoordinador(
            est.getCarrera() != null ? est.getCarrera().getIdCarrera() : null);

        List<TrazabilidadEstudianteDto.EventoTrazabilidadDto> eventos = new ArrayList<>();

        for (MatriculaTitulacion m : matriculaRepo.findByEstudiante_IdEstudiante(idEstudiante)) {
            eventos.add(evento("Matrícula (" + m.getNivel() + ")",
                m.getFechaMatricula(), "Estudiante", m.getEstado() != null ? m.getEstado().toString() : "-",
                "Opción: " + (m.getOpcionTitulacion() != null ? m.getOpcionTitulacion().getNombre() : "-")));
        }

        for (EleccionTitulacion e : eleccionRepo.findByEstudiante_IdEstudiante(idEstudiante)) {
            eventos.add(evento("Selección de opción de titulación",
                fechaHora(e.getFechaEleccion()), "Estudiante", e.getEstado() != null ? e.getEstado().toString() : "-",
                e.getModalidad() != null ? e.getModalidad().getNombre() : "-"));
        }

        for (PropuestaTitulacion p : propuestaRepo.findByEstudiante_IdEstudiante(idEstudiante)) {
            eventos.add(evento("Propuesta de titulación",
                fechaHora(p.getFechaEnvio()), "Estudiante", p.getEstado(), p.getTitulo()));
            if (p.getFechaRevision() != null) {
                eventos.add(evento("Revisión de propuesta",
                    fechaHora(p.getFechaRevision()), "Comisión de Titulación", p.getEstado(), p.getObservacionesComision()));
            }
        }

        List<AnteproyectoTitulacion> anteproyectos = anteproyectoRepo.findByEstudiante_IdEstudiante(idEstudiante);
        for (AnteproyectoTitulacion a : anteproyectos) {
            eventos.add(evento("Anteproyecto",
                fechaHora(a.getFechaCreacion()), "Comisión de Titulación",
                a.getEstado() != null ? a.getEstado().toString() : "-", a.getMotivoModificacionSolicitada()));
            for (TutoriaAnteproyecto t : tutoriaRepo.findByAnteproyecto_IdAnteproyectoOrderByFechaAsc(a.getIdAnteproyecto())) {
                eventos.add(evento("Asesoría de anteproyecto",
                    fechaHora(t.getFecha()),
                    t.getDocente() != null ? nombreCompleto(t.getDocente().getUsuario()) : "-",
                    t.getEstado(), t.getModalidad()));
            }
        }

        proyectoRepo.findFirstByPropuesta_Estudiante_IdEstudianteOrderByIdProyectoDesc(idEstudiante).ifPresent(proyecto -> {
            Integer idProyecto = proyecto.getIdProyecto();

            for (BitacoraAsignacion b : bitacoraRepo.findByProyecto_IdProyectoOrderByFechaDesc(idProyecto)) {
                eventos.add(evento("Asignación " + b.getTipoAsignacion(),
                    b.getFecha(), b.getRealizadoPor() != null ? nombreCompleto(b.getRealizadoPor()) : "-",
                    b.getCargo() != null ? b.getCargo() : b.getNombreAsignado(), b.getObservacion()));
            }

            for (AsesoriaDirector a : asesoriaRepo.findByProyecto_IdProyectoOrderByFechaDesc(idProyecto)) {
                eventos.add(evento("Asesoría de director (corte " + a.getNumeroCorte() + ")",
                    a.getFecha(), a.getDirector() != null ? nombreCompleto(a.getDirector().getUsuario()) : "-",
                    a.getPorcentajeAvance() != null ? a.getPorcentajeAvance() + "% avance" : "-", a.getObservaciones()));
            }

            if (proyecto.getFechaVerificacionAntiplagio() != null) {
                eventos.add(evento("Verificación antiplagio",
                    fechaHora(proyecto.getFechaVerificacionAntiplagio()), "Sistema",
                    proyecto.getPorcentajeAntiplagio() + "% coincidencia", null));
            }

            for (Sustentacion s : sustentacionRepo.findByProyecto_IdProyectoOrderByFechaDescHoraDesc(idProyecto)) {
                eventos.add(evento(s.getTipo() != null ? s.getTipo() : "Sustentación",
                    fechaHora(s.getFecha()), "Tribunal", s.getObservaciones(), s.getLugar()));
            }

            eventos.add(evento("Estado del proyecto de titulación",
                null, "Sistema", proyecto.getEstado(), proyecto.getTitulo()));
        });

        actaGradoRepo.findByEstudiante_IdEstudiante(idEstudiante).ifPresent(acta ->
            eventos.add(evento("Acta de grado",
                fechaHora(acta.getFechaEmision()), "Dirección Académica",
                acta.getEstado() != null ? acta.getEstado().toString() : "-",
                acta.getNotaFinalGrado() != null ? "Nota final: " + acta.getNotaFinalGrado() : null)));

        tituloRepo.findByEstudiante_IdEstudiante(idEstudiante).ifPresent(titulo ->
            eventos.add(evento("Incorporación / título profesional",
                fechaHora(titulo.getFechaEmision()), "Secretaría General",
                titulo.getEstado() != null ? titulo.getEstado().toString() : "-",
                titulo.getRegistroSenescyt() != null ? "SENESCYT: " + titulo.getRegistroSenescyt() : null)));

        eventos.sort(Comparator.comparing(
            TrazabilidadEstudianteDto.EventoTrazabilidadDto::getFecha,
            Comparator.nullsLast(Comparator.naturalOrder())));

        return TrazabilidadEstudianteDto.builder()
            .idEstudiante(idEstudiante)
            .estudiante(nombreCompleto(est.getUsuario()))
            .cedula(est.getUsuario().getCedula())
            .carrera(est.getCarrera() != null ? est.getCarrera().getNombre() : "-")
            .eventos(eventos)
            .build();
    }

    private static TrazabilidadEstudianteDto.EventoTrazabilidadDto evento(
            String etapa, LocalDateTime fecha, String actor, String estadoResultante, String detalle) {
        return TrazabilidadEstudianteDto.EventoTrazabilidadDto.builder()
            .etapa(etapa).fecha(fecha).actor(actor).estadoResultante(estadoResultante).detalle(detalle).build();
    }

    private static LocalDateTime fechaHora(LocalDate d) { return d == null ? null : d.atStartOfDay(); }
    private static LocalDateTime fechaHora(LocalDateTime d) { return d; }

    private static String nombreCompleto(Usuario u) {
        if (u == null) return "-";
        return (u.getNombres() != null ? u.getNombres() : "") + " " + (u.getApellidos() != null ? u.getApellidos() : "");
    }

    // ── 2.3 DASHBOARD GENERAL ────────────────────────────────────────────────

    public DashboardReportesDto getDashboard(String periodo) {
        List<Object[]> matriculaFilas = matriculaRepo.conteoPorNivel(periodo);
        int totalMatriculados = 0;
        List<DashboardReportesDto.ConteoDto> matriculadosPorNivel = new ArrayList<>();
        for (Object[] fila : matriculaFilas) {
            long cantidad = numOrZero(fila[1]);
            totalMatriculados += cantidad;
            matriculadosPorNivel.add(DashboardReportesDto.ConteoDto.builder()
                .etiqueta(fila[0].toString()).cantidad(cantidad).build());
        }

        List<Object[]> ticFilas = proyectoRepo.resumenPorModalidad(periodo);
        List<Object[]> complexivoResumen = examenRepo.resumenGlobal(periodo, NOTA_APROBACION);

        List<DashboardReportesDto.ModalidadConteoDto> porModalidad = new ArrayList<>();
        long aprobadosTotal = 0, reprobadosTotal = 0, enProgresoTotal = 0;
        for (Object[] fila : ticFilas) {
            long total = numOrZero(fila[1]);
            long aprob = numOrZero(fila[2]);
            long repr = numOrZero(fila[3]);
            long enProceso = numOrZero(fila[4]);
            aprobadosTotal += aprob; reprobadosTotal += repr; enProgresoTotal += enProceso;
            porModalidad.add(DashboardReportesDto.ModalidadConteoDto.builder()
                .modalidad(fila[0].toString()).total(total).aprobados(aprob).reprobados(repr).enProceso(enProceso).build());
        }
        if (!complexivoResumen.isEmpty() && complexivoResumen.get(0)[0] != null) {
            Object[] fila = complexivoResumen.get(0);
            long total = numOrZero(fila[0]);
            long aprob = numOrZero(fila[1]);
            long repr = numOrZero(fila[2]);
            aprobadosTotal += aprob; reprobadosTotal += repr;
            porModalidad.add(DashboardReportesDto.ModalidadConteoDto.builder()
                .modalidad("EXAMEN_COMPLEXIVO").total(total).aprobados(aprob).reprobados(repr).enProceso(0L).build());
        }

        long totalEvaluados = aprobadosTotal + reprobadosTotal;
        BigDecimal promedioNotaGrado = actaGradoRepo.promedioNotaFinalGradoGlobal();

        List<Object[]> fechas = matriculaRepo.fechasMatriculaYGradoParaTiempoPromedio();
        Double tiempoPromedioDias = null;
        if (!fechas.isEmpty()) {
            double total = 0; int n = 0;
            for (Object[] fila : fechas) {
                LocalDateTime inicio = (LocalDateTime) fila[0];
                LocalDate fin = (LocalDate) fila[1];
                if (inicio != null && fin != null) {
                    total += ChronoUnit.DAYS.between(inicio.toLocalDate(), fin);
                    n++;
                }
            }
            tiempoPromedioDias = n > 0 ? total / n : null;
        }

        return DashboardReportesDto.builder()
            .periodo(periodo)
            .totalMatriculados(totalMatriculados)
            .matriculadosPorNivel(matriculadosPorNivel)
            .enProgreso((int) enProgresoTotal)
            .aprobados((int) aprobadosTotal)
            .reprobados((int) reprobadosTotal)
            .tasaAprobacion(totalEvaluados > 0 ? (aprobadosTotal * 100.0 / totalEvaluados) : 0.0)
            .promedioNotaGrado(promedioNotaGrado != null ? redondear(promedioNotaGrado) : null)
            .tiempoPromedioTitulacionDias(tiempoPromedioDias)
            .porModalidad(porModalidad)
            .build();
    }

    // ── 2.4 REPORTE DE EXPEDIENTES ───────────────────────────────────────────

    public ReporteExpedientesDto getReporteExpedientes(Integer idCarrera, String periodo) {
        authorizationService.exigirPropiaCarreraSiCoordinador(idCarrera);
        List<Object[]> resumenEstado = expedienteRepo.resumenPorEstado(idCarrera, periodo);
        int total = 0, incompletos = 0, completos = 0, validados = 0, legalizados = 0;
        for (Object[] fila : resumenEstado) {
            String estado = fila[0].toString();
            long cantidad = numOrZero(fila[1]);
            total += cantidad;
            switch (estado) {
                case "INCOMPLETO" -> incompletos = (int) cantidad;
                case "COMPLETO" -> completos = (int) cantidad;
                case "VALIDADO" -> validados = (int) cantidad;
                case "LEGALIZADO" -> legalizados = (int) cantidad;
                default -> { }
            }
        }

        List<Object[]> faltantes = documentoExpedienteRepo.documentosFaltantesFrecuentes(idCarrera, periodo);
        List<ReporteExpedientesDto.DocumentoFaltanteDto> documentosFaltantes = faltantes.stream()
            .map(f -> ReporteExpedientesDto.DocumentoFaltanteDto.builder()
                .documento((String) f[0]).vecesFaltante(((Number) f[1]).longValue()).build())
            .collect(Collectors.toList());

        String nombreCarrera = idCarrera != null
            ? carreraRepo.findById(idCarrera).map(Carrera::getNombre).orElse(null)
            : null;

        return ReporteExpedientesDto.builder()
            .carrera(nombreCarrera)
            .periodo(periodo)
            .totalExpedientes(total)
            .incompletos(incompletos)
            .completos(completos)
            .validados(validados)
            .legalizados(legalizados)
            .documentosMasFaltantes(documentosFaltantes)
            .build();
    }
}
