package com.erwin.backend.scheduler;

import com.erwin.backend.entities.PropuestaTitulacion;
import com.erwin.backend.repository.PropuestaTitulacionRepository;
import com.erwin.backend.util.CalendarioAcademicoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Alerta (log) cuando el plazo de la Comisión (5 días hábiles para trasladar
 * al Consejo Directivo) o del Consejo Directivo (10 días hábiles para
 * resolver) de una propuesta del TIC está por vencer o ya venció. El sistema
 * no tiene hoy un mecanismo de notificación push a los propios órganos
 * colegiados: el canal disponible es el log de la aplicación (auditable) más
 * la alerta equivalente que ya se registra al momento de resolver, en
 * PropuestaResolucionService.
 *
 * Configurable vía application.properties:
 *   app.scheduler.plazos-titulacion.enabled=true|false
 *   app.scheduler.plazos-titulacion.cron=0 0 6 * * *
 */
@Component
@ConditionalOnProperty(
        name = "app.scheduler.plazos-titulacion.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PlazosTitulacionScheduler {

    private static final Logger log = LoggerFactory.getLogger(PlazosTitulacionScheduler.class);
    private static final long DIAS_HABILES_ALERTA_TRASLADO_COMISION = 5;
    private static final long DIAS_HABILES_LIMITE_RESOLUCION_CONSEJO = 10;

    private final PropuestaTitulacionRepository propuestaRepo;

    public PlazosTitulacionScheduler(PropuestaTitulacionRepository propuestaRepo) {
        this.propuestaRepo = propuestaRepo;
    }

    @Scheduled(cron = "${app.scheduler.plazos-titulacion.cron:0 0 6 * * *}")
    @Transactional(readOnly = true)
    public void ejecutar() {
        List<PropuestaTitulacion> pendientes = propuestaRepo.findByEstado("APROBADA").stream()
                .filter(p -> !Boolean.TRUE.equals(p.getResolucionConsejoDirectivo()))
                .filter(p -> p.getFechaRecepcionComision() != null)
                .toList();

        LocalDateTime ahora = LocalDateTime.now();
        for (PropuestaTitulacion p : pendientes) {
            long dias = CalendarioAcademicoUtil.diasHabilesEntre(p.getFechaRecepcionComision(), ahora);

            if (dias > DIAS_HABILES_LIMITE_RESOLUCION_CONSEJO) {
                log.warn("Propuesta {}: plazo de {} días hábiles del Consejo Directivo VENCIDO ({} días hábiles transcurridos desde la recepción de Comisión)",
                        p.getIdPropuesta(), DIAS_HABILES_LIMITE_RESOLUCION_CONSEJO, dias);
            } else if (dias >= DIAS_HABILES_LIMITE_RESOLUCION_CONSEJO - 1) {
                log.warn("Propuesta {}: el plazo del Consejo Directivo está por vencer ({} de {} días hábiles transcurridos)",
                        p.getIdPropuesta(), dias, DIAS_HABILES_LIMITE_RESOLUCION_CONSEJO);
            } else if (dias >= DIAS_HABILES_ALERTA_TRASLADO_COMISION) {
                log.info("Propuesta {}: superó el plazo de traslado de la Comisión ({} de {} días hábiles); el Consejo Directivo debe resolver antes de {} días hábiles",
                        p.getIdPropuesta(), dias, DIAS_HABILES_ALERTA_TRASLADO_COMISION, DIAS_HABILES_LIMITE_RESOLUCION_CONSEJO);
            }
        }
    }
}
