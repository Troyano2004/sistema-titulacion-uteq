package com.erwin.backend.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

/**
 * Aritmética de semanas/días hábiles del periodo académico, usada para los
 * plazos reglamentarios de Módulo 6 (propuesta del TIC): semana 01-04 para
 * presentar la propuesta, 5 días hábiles de la Comisión para el traslado,
 * 10 días hábiles del Consejo Directivo para resolver.
 *
 * No hay calendario de feriados en el sistema: "día hábil" aquí solo excluye
 * sábado y domingo.
 */
public final class CalendarioAcademicoUtil {

    private CalendarioAcademicoUtil() {}

    /** Semana del periodo (1-indexada) correspondiente a `hoy`, contada desde `fechaInicio`. */
    public static int semanaActual(LocalDate fechaInicio, LocalDate hoy) {
        if (fechaInicio == null) {
            throw new IllegalStateException("El periodo de titulación no tiene fecha de inicio configurada");
        }
        long dias = java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, hoy);
        if (dias < 0) return 0;
        return (int) (dias / 7) + 1;
    }

    /** Días hábiles (lunes-viernes) transcurridos entre dos instantes, sin contar el de inicio. */
    public static long diasHabilesEntre(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null) return 0;
        LocalDate d = desde.toLocalDate();
        LocalDate h = hasta.toLocalDate();
        long dias = 0;
        while (d.isBefore(h)) {
            d = d.plusDays(1);
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                dias++;
            }
        }
        return dias;
    }

    /** Suma N días hábiles (lunes-viernes) a una fecha. */
    public static LocalDate sumarDiasHabiles(LocalDate fecha, int dias) {
        return sumarDiasHabiles(fecha, dias, Collections.emptySet());
    }

    /** Suma N días hábiles (lunes-viernes, excluyendo feriados) a una fecha — Módulo 14. */
    public static LocalDate sumarDiasHabiles(LocalDate fecha, int dias, Set<LocalDate> feriados) {
        if (fecha == null) return null;
        LocalDate d = fecha;
        int agregados = 0;
        while (agregados < dias) {
            d = d.plusDays(1);
            if (esDiaHabil(d, feriados)) {
                agregados++;
            }
        }
        return d;
    }

    private static boolean esDiaHabil(LocalDate d, Set<LocalDate> feriados) {
        return d.getDayOfWeek() != DayOfWeek.SATURDAY
                && d.getDayOfWeek() != DayOfWeek.SUNDAY
                && (feriados == null || !feriados.contains(d));
    }

    private static final int HORA_INICIO_JORNADA = 8;
    private static final int HORA_FIN_JORNADA = 17;
    private static final int HORA_INICIO_ALMUERZO = 12;
    private static final int HORA_FIN_ALMUERZO = 13;

    /**
     * Suma N horas laborables (lunes-viernes, 08:00-17:00 con almuerzo
     * 12:00-13:00 excluido, sin feriados) a un instante — Módulo 14,
     * plazo de 72 horas laborables para justificar inasistencia al
     * Examen Complexivo.
     */
    public static LocalDateTime sumarHorasLaborables(LocalDateTime desde, int horas, Set<LocalDate> feriados) {
        if (desde == null) return null;
        LocalDateTime cursor = desde;
        int restantes = horas;
        while (restantes > 0) {
            if (esHoraLaborable(cursor, feriados)) {
                restantes--;
            }
            cursor = cursor.plusHours(1);
        }
        return cursor;
    }

    private static boolean esHoraLaborable(LocalDateTime dt, Set<LocalDate> feriados) {
        if (!esDiaHabil(dt.toLocalDate(), feriados)) return false;
        int hora = dt.getHour();
        if (hora < HORA_INICIO_JORNADA || hora >= HORA_FIN_JORNADA) return false;
        return hora < HORA_INICIO_ALMUERZO || hora >= HORA_FIN_ALMUERZO;
    }
}
