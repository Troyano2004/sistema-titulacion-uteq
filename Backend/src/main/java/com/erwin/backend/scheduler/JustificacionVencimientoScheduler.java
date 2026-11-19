package com.erwin.backend.scheduler;

import com.erwin.backend.service.JustificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TAREA 2.4: diariamente, marca VENCIDA cualquier {@code SolicitudJustificacion}
 * PENDIENTE cuya fechaLimite ya pasó, y registra el REPROBADO automático
 * correspondiente vía OportunidadService.
 *
 * Configurable vía application.properties:
 *   app.scheduler.justificacion-vencimiento.enabled=true|false
 *   app.scheduler.justificacion-vencimiento.cron=0 0 4 * * *
 */
@Component
@ConditionalOnProperty(
        name = "app.scheduler.justificacion-vencimiento.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class JustificacionVencimientoScheduler {

    private static final Logger log = LoggerFactory.getLogger(JustificacionVencimientoScheduler.class);

    private final JustificacionService justificacionService;

    public JustificacionVencimientoScheduler(JustificacionService justificacionService) {
        this.justificacionService = justificacionService;
    }

    @Scheduled(cron = "${app.scheduler.justificacion-vencimiento.cron:0 0 4 * * *}")
    public void ejecutar() {
        List<Long> vencidas = justificacionService.marcarVencidas();
        if (!vencidas.isEmpty()) {
            log.info("Justificaciones marcadas VENCIDA: {} ({})", vencidas.size(), vencidas);
        }
    }
}
