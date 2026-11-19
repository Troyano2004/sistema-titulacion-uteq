package com.erwin.backend.scheduler;

import com.erwin.backend.service.ExamenComplexivoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TAREA 3.3: cada minuto, finaliza automáticamente los exámenes EN_CURSO
 * cuya horaFin (temporizador de servidor) ya venció, calificando con las
 * respuestas registradas hasta ese momento.
 *
 * Configurable vía application.properties:
 *   app.scheduler.examen-complexivo-timeout.enabled=true|false
 *   app.scheduler.examen-complexivo-timeout.cron=0 * * * * *
 */
@Component
@ConditionalOnProperty(
        name = "app.scheduler.examen-complexivo-timeout.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ExamenComplexivoTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExamenComplexivoTimeoutScheduler.class);

    private final ExamenComplexivoService examenService;

    public ExamenComplexivoTimeoutScheduler(ExamenComplexivoService examenService) {
        this.examenService = examenService;
    }

    @Scheduled(cron = "${app.scheduler.examen-complexivo-timeout.cron:0 * * * * *}")
    public void ejecutar() {
        List<Long> finalizados = examenService.finalizarExamenesVencidos();
        if (!finalizados.isEmpty()) {
            log.info("Timeout de Examen Complexivo: finalizados automáticamente {} exámenes: {}",
                    finalizados.size(), finalizados);
        }
    }
}
