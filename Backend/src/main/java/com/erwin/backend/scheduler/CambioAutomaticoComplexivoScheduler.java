package com.erwin.backend.scheduler;

import com.erwin.backend.entities.Coordinador;
import com.erwin.backend.entities.EleccionTitulacion;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.MatriculaTitulacion;
import com.erwin.backend.entities.Modalidadtitulacion;
import com.erwin.backend.entities.PeriodoTitulacion;
import com.erwin.backend.enums.EstadoMatricula;
import com.erwin.backend.enums.NivelMatricula;
import com.erwin.backend.repository.CoordinadorRepository;
import com.erwin.backend.repository.EleccionTitulacionRepository;
import com.erwin.backend.repository.MatriculaTitulacionRepository;
import com.erwin.backend.repository.ModalidadTitulacionRepository;
import com.erwin.backend.repository.PeriodoTitulacionRepository;
import com.erwin.backend.repository.PropuestaTitulacionRepository;
import com.erwin.backend.service.EleccionService;
import com.erwin.backend.service.EmailService;
import com.erwin.backend.util.CalendarioAcademicoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Reglamento UIC-UTEQ, Art. 50: si un estudiante matriculado en Titulación I
 * bajo la opción TIC no presenta su propuesta hasta la semana 04 del
 * periodo, su opción cambia automáticamente a Examen Complexivo.
 *
 * Configurable vía application.properties:
 *   app.scheduler.cambio-automatico-complexivo.enabled=true|false
 *   app.scheduler.cambio-automatico-complexivo.cron=0 0 3 * * *
 */
@Component
@ConditionalOnProperty(
        name = "app.scheduler.cambio-automatico-complexivo.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class CambioAutomaticoComplexivoScheduler {

    private static final Logger log = LoggerFactory.getLogger(CambioAutomaticoComplexivoScheduler.class);
    private static final String NOMBRE_MODALIDAD_COMPLEXIVO = "Examen Complexivo";
    private static final int SEMANA_LIMITE_PRESENTACION_PROPUESTA_TIC = 4;
    private static final String MOTIVO_CAMBIO_AUTOMATICO =
            "Cambio automático por incumplimiento de plazo — Art. 50 del Reglamento";

    private final PeriodoTitulacionRepository periodoRepo;
    private final MatriculaTitulacionRepository matriculaRepo;
    private final ModalidadTitulacionRepository modalidadRepo;
    private final PropuestaTitulacionRepository propuestaRepo;
    private final EleccionTitulacionRepository eleccionRepo;
    private final CoordinadorRepository coordinadorRepo;
    private final EleccionService eleccionService;
    private final EmailService emailService;

    public CambioAutomaticoComplexivoScheduler(PeriodoTitulacionRepository periodoRepo,
                                                MatriculaTitulacionRepository matriculaRepo,
                                                ModalidadTitulacionRepository modalidadRepo,
                                                PropuestaTitulacionRepository propuestaRepo,
                                                EleccionTitulacionRepository eleccionRepo,
                                                CoordinadorRepository coordinadorRepo,
                                                EleccionService eleccionService,
                                                EmailService emailService) {
        this.periodoRepo = periodoRepo;
        this.matriculaRepo = matriculaRepo;
        this.modalidadRepo = modalidadRepo;
        this.propuestaRepo = propuestaRepo;
        this.eleccionRepo = eleccionRepo;
        this.coordinadorRepo = coordinadorRepo;
        this.eleccionService = eleccionService;
        this.emailService = emailService;
    }

    @Scheduled(cron = "${app.scheduler.cambio-automatico-complexivo.cron:0 0 3 * * *}")
    @Transactional
    public void ejecutar() {
        PeriodoTitulacion periodo = periodoRepo.findByActivoTrue().orElse(null);
        if (periodo == null || periodo.getFechaInicio() == null) return;

        int semana = CalendarioAcademicoUtil.semanaActual(periodo.getFechaInicio(), LocalDate.now());
        if (semana <= SEMANA_LIMITE_PRESENTACION_PROPUESTA_TIC) return;

        Modalidadtitulacion complexivo = modalidadRepo.findByNombreIgnoreCase(NOMBRE_MODALIDAD_COMPLEXIVO).orElse(null);
        if (complexivo == null) {
            log.warn("Cambio automático a Complexivo: no existe la modalidad '{}' en el catálogo, se omite la ejecución",
                    NOMBRE_MODALIDAD_COMPLEXIVO);
            return;
        }

        List<MatriculaTitulacion> candidatas = matriculaRepo.findByPeriodoAcademicoAndNivelAndEstado(
                periodo.getDescripcion(), NivelMatricula.TITULACION_I, EstadoMatricula.ACTIVA);

        for (MatriculaTitulacion matricula : candidatas) {
            try {
                procesar(matricula, periodo, complexivo);
            } catch (Exception ex) {
                log.error("Cambio automático a Complexivo: error procesando matrícula {}: {}",
                        matricula.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void procesar(MatriculaTitulacion matricula, PeriodoTitulacion periodo, Modalidadtitulacion complexivo) {
        if (matricula.getOpcionTitulacion() != null
                && NOMBRE_MODALIDAD_COMPLEXIVO.equalsIgnoreCase(matricula.getOpcionTitulacion().getNombre())) {
            return; // ya está en Complexivo
        }

        Estudiante estudiante = matricula.getEstudiante();
        EleccionTitulacion eleccion = eleccionRepo
                .findByEstudiante_IdEstudianteAndPeriodo_IdPeriodo(estudiante.getIdEstudiante(), periodo.getIdPeriodo())
                .orElse(null);

        boolean presentoPropuesta = eleccion != null
                && propuestaRepo.findByEleccion_IdEleccion(eleccion.getIdEleccion()).isPresent();
        if (presentoPropuesta) return;

        matricula.setOpcionTitulacion(complexivo);
        matricula.setSubtipoTrabajo(null);
        matricula.setMotivoUltimoCambio(MOTIVO_CAMBIO_AUTOMATICO);
        matricula.setFechaUltimoCambio(LocalDateTime.now());
        matriculaRepo.save(matricula);

        if (eleccion != null) {
            eleccionService.elegir(estudiante.getIdEstudiante(), complexivo.getIdModalidad(), MOTIVO_CAMBIO_AUTOMATICO);
        }

        log.info("Cambio automático a Complexivo aplicado: matrícula {} (estudiante {})",
                matricula.getId(), estudiante.getIdEstudiante());

        notificar(estudiante, complexivo);
    }

    private void notificar(Estudiante estudiante, Modalidadtitulacion complexivo) {
        if (estudiante.getUsuario() == null) return;
        String nombreEstudiante = (n(estudiante.getUsuario().getNombres()) + " " + n(estudiante.getUsuario().getApellidos())).trim();

        String correoEstudiante = estudiante.getUsuario().getCorreoInstitucional();
        if (correoEstudiante != null && !correoEstudiante.isBlank()) {
            emailService.notificarCambioOpcionTitulacion(
                    correoEstudiante, nombreEstudiante, complexivo.getNombre(), MOTIVO_CAMBIO_AUTOMATICO);
        }

        if (estudiante.getCarrera() == null) return;
        Coordinador coordinador = coordinadorRepo
                .findFirstByCarrera_IdCarreraAndActivoTrue(estudiante.getCarrera().getIdCarrera())
                .orElse(null);
        if (coordinador == null || coordinador.getUsuario() == null) return;
        String correoCoordinador = coordinador.getUsuario().getCorreoInstitucional();
        if (correoCoordinador != null && !correoCoordinador.isBlank()) {
            emailService.notificarCambioOpcionTitulacion(
                    correoCoordinador, nombreEstudiante, complexivo.getNombre(), MOTIVO_CAMBIO_AUTOMATICO);
        }
    }

    private String n(String s) { return s == null ? "" : s; }
}
