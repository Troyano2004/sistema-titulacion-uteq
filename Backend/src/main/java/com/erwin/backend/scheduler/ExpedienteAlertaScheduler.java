package com.erwin.backend.scheduler;

import com.erwin.backend.dtos.ExpedienteDtos.AlertaExpedienteDto;
import com.erwin.backend.entities.Estudiante;
import com.erwin.backend.entities.Usuario;
import com.erwin.backend.repository.EstudianteRepository;
import com.erwin.backend.repository.UsuarioRepository;
import com.erwin.backend.service.EmailService;
import com.erwin.backend.service.ExpedienteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TAREA 1.4: alertas proactivas de expediente — diariamente identifica
 * documentos PENDIENTE/RECHAZADO cuyo evento (sustentación/examen complexivo)
 * está a &lt;= 15 días y notifica al estudiante y a Secretaría-Abogacía.
 *
 * Configurable vía application.properties:
 *   app.scheduler.expediente-alerta.enabled=true|false
 *   app.scheduler.expediente-alerta.cron=0 0 7 * * *
 */
@Component
@ConditionalOnProperty(
        name = "app.scheduler.expediente-alerta.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ExpedienteAlertaScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpedienteAlertaScheduler.class);
    private static final int DIAS_UMBRAL = 15;
    private static final String ROL_SECRETARIO = "SECRETARIO_ABOGADO";

    private final ExpedienteService expedienteService;
    private final EstudianteRepository estudianteRepo;
    private final UsuarioRepository usuarioRepo;
    private final EmailService emailService;

    public ExpedienteAlertaScheduler(ExpedienteService expedienteService,
                                      EstudianteRepository estudianteRepo,
                                      UsuarioRepository usuarioRepo,
                                      EmailService emailService) {
        this.expedienteService = expedienteService;
        this.estudianteRepo = estudianteRepo;
        this.usuarioRepo = usuarioRepo;
        this.emailService = emailService;
    }

    @Scheduled(cron = "${app.scheduler.expediente-alerta.cron:0 0 7 * * *}")
    public void ejecutar() {
        List<AlertaExpedienteDto> alertas = expedienteService.alertasProximasSistema(DIAS_UMBRAL);
        if (alertas.isEmpty()) return;

        List<Usuario> secretarios = usuarioRepo.findByRolIgnoreCase(ROL_SECRETARIO);
        log.info("Expediente: {} documento(s) pendientes con evento a <= {} días", alertas.size(), DIAS_UMBRAL);

        for (AlertaExpedienteDto alerta : alertas) {
            try {
                notificarEstudiante(alerta);
                for (Usuario secretario : secretarios) {
                    notificarSecretario(secretario, alerta);
                }
            } catch (Exception ex) {
                log.error("Error notificando alerta de expediente {}: {}", alerta.idDocumentoExpediente(), ex.getMessage(), ex);
            }
        }
    }

    private void notificarEstudiante(AlertaExpedienteDto alerta) {
        Estudiante estudiante = estudianteRepo.findById(alerta.idEstudiante()).orElse(null);
        if (estudiante == null || estudiante.getUsuario() == null) return;
        String correo = estudiante.getUsuario().getCorreoInstitucional();
        if (correo == null || correo.isBlank()) return;
        emailService.notificarDocumentoExpedientePendiente(
                correo, alerta.nombreEstudiante(), alerta.nombreEstudiante(), alerta.nombreDocumento(), alerta.diasRestantes());
    }

    private void notificarSecretario(Usuario secretario, AlertaExpedienteDto alerta) {
        String correo = secretario.getCorreoInstitucional();
        if (correo == null || correo.isBlank()) return;
        String nombreSecretario = (n(secretario.getNombres()) + " " + n(secretario.getApellidos())).trim();
        emailService.notificarDocumentoExpedientePendiente(
                correo, nombreSecretario, alerta.nombreEstudiante(), alerta.nombreDocumento(), alerta.diasRestantes());
    }

    private String n(String s) { return s == null ? "" : s; }
}
