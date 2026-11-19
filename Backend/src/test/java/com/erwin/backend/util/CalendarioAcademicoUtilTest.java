package com.erwin.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aritmética de plazos del Módulo 6 (propuesta del TIC): la ventana de
 * presentación es semana 01-04 del periodo, contada desde
 * PeriodoTitulacion.fechaInicio (ver ComisionTemasController.
 * validarPlazoPresentacionPropuestaTic, que se apoya en semanaActual()).
 */
class CalendarioAcademicoUtilTest {

    private static final int SEMANA_INICIO = 1;
    private static final int SEMANA_LIMITE = 4;

    private boolean dentroDePlazo(LocalDate fechaInicioPeriodo, LocalDate hoy) {
        int semana = CalendarioAcademicoUtil.semanaActual(fechaInicioPeriodo, hoy);
        return semana >= SEMANA_INICIO && semana <= SEMANA_LIMITE;
    }

    @Test
    @DisplayName("El primer día del periodo está en semana 1 (dentro de plazo)")
    void primerDiaEsSemanaUno() {
        LocalDate inicio = LocalDate.of(2026, 3, 2);
        assertThat(CalendarioAcademicoUtil.semanaActual(inicio, inicio)).isEqualTo(1);
        assertThat(dentroDePlazo(inicio, inicio)).isTrue();
    }

    @Test
    @DisplayName("El día 27 (último día de la semana 4) sigue dentro de plazo")
    void ultimoDiaDeSemanaCuatroDentroDePlazo() {
        LocalDate inicio = LocalDate.of(2026, 3, 2);
        LocalDate hoy = inicio.plusDays(27); // día 28 del periodo -> semana 4
        assertThat(CalendarioAcademicoUtil.semanaActual(inicio, hoy)).isEqualTo(4);
        assertThat(dentroDePlazo(inicio, hoy)).isTrue();
    }

    @Test
    @DisplayName("El primer día de la semana 5 ya está fuera de plazo")
    void primerDiaDeSemanaCincoFueraDePlazo() {
        LocalDate inicio = LocalDate.of(2026, 3, 2);
        LocalDate hoy = inicio.plusDays(28); // día 29 del periodo -> semana 5
        assertThat(CalendarioAcademicoUtil.semanaActual(inicio, hoy)).isEqualTo(5);
        assertThat(dentroDePlazo(inicio, hoy)).isFalse();
    }

    @Test
    @DisplayName("Una fecha anterior al inicio del periodo se trata como semana 0 (fuera de plazo)")
    void fechaAnteriorAlInicioEsSemanaCero() {
        LocalDate inicio = LocalDate.of(2026, 3, 2);
        LocalDate hoy = inicio.minusDays(1);
        assertThat(CalendarioAcademicoUtil.semanaActual(inicio, hoy)).isZero();
        assertThat(dentroDePlazo(inicio, hoy)).isFalse();
    }

    @Test
    @DisplayName("diasHabilesEntre cuenta solo lunes-viernes, sin contar el día de inicio")
    void diasHabilesExcluyeFinDeSemana() {
        // Lunes 2026-03-02 -> Lunes 2026-03-09: 5 días hábiles (mar,mié,jue,vie + el lunes siguiente)
        LocalDateTime desde = LocalDateTime.of(2026, 3, 2, 8, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 3, 9, 8, 0);
        assertThat(CalendarioAcademicoUtil.diasHabilesEntre(desde, hasta)).isEqualTo(5);
    }

    // ── Módulo 14: plazo de 72 horas laborables (inasistencia al Complexivo) ──

    @Test
    @DisplayName("sumarHorasLaborables cuenta solo horas de jornada (08-17, sin almuerzo) de lunes a viernes")
    void sumarHorasLaborablesRespetaJornadaLaboral() {
        // Lunes 08:00 + 4 horas laborables (08,09,10,11) -> 12:00 (justo antes del almuerzo)
        LocalDateTime desde = LocalDateTime.of(2026, 3, 2, 8, 0); // lunes
        LocalDateTime resultado = CalendarioAcademicoUtil.sumarHorasLaborables(desde, 4, Set.of());
        assertThat(resultado).isEqualTo(LocalDateTime.of(2026, 3, 2, 12, 0));
    }

    @Test
    @DisplayName("sumarHorasLaborables salta el fin de semana (viernes fuera de jornada -> lunes)")
    void sumarHorasLaborablesSaltaFinDeSemana() {
        // Viernes 17:00 (jornada ya terminada) + 1 hora laborable: sábado y domingo
        // no cuentan, la siguiente hora hábil es el lunes a las 08:00.
        LocalDateTime desde = LocalDateTime.of(2026, 3, 6, 17, 0); // viernes, fin de jornada
        LocalDateTime resultado = CalendarioAcademicoUtil.sumarHorasLaborables(desde, 1, Set.of());
        assertThat(resultado).isEqualTo(LocalDateTime.of(2026, 3, 9, 9, 0)); // lunes 09:00
    }

    @Test
    @DisplayName("sumarHorasLaborables(72) desde el lunes agota exactamente 9 jornadas completas (8h/día)")
    void sumarHorasLaborablesSetentaYDosHorasEquivaleANueveJornadas() {
        LocalDateTime desde = LocalDateTime.of(2026, 3, 2, 8, 0); // lunes
        LocalDateTime resultado = CalendarioAcademicoUtil.sumarHorasLaborables(desde, 72, Set.of());
        // 8 horas laborables/día -> 72/8 = 9 jornadas: lunes-viernes (5) + fin de semana + lunes-jueves (4)
        assertThat(resultado.toLocalDate()).isEqualTo(LocalDate.of(2026, 3, 12)); // jueves de la semana siguiente
        assertThat(resultado.toLocalDate().getDayOfWeek().getValue()).isEqualTo(4); // jueves
    }

    // ── Módulo 14: plazo de 3 días hábiles (inasistencia a sustentación) ──────

    @Test
    @DisplayName("sumarDiasHabiles(3) excluye sábado y domingo")
    void sumarTresDiasHabilesExcluyeFinDeSemana() {
        LocalDate viernes = LocalDate.of(2026, 3, 6);
        LocalDate resultado = CalendarioAcademicoUtil.sumarDiasHabiles(viernes, 3, Set.of());
        // vie -> sáb/dom no cuentan -> lun, mar, mié
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 3, 11));
    }

    @Test
    @DisplayName("sumarDiasHabiles(3) también excluye los feriados configurados")
    void sumarTresDiasHabilesExcluyeFeriados() {
        LocalDate viernes = LocalDate.of(2026, 3, 6);
        LocalDate feriado = LocalDate.of(2026, 3, 9); // el lunes siguiente es feriado
        LocalDate resultado = CalendarioAcademicoUtil.sumarDiasHabiles(viernes, 3, Set.of(feriado));
        // vie -> sáb/dom/feriado(lun) no cuentan -> mar, mié, jue
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 3, 12));
    }
}
