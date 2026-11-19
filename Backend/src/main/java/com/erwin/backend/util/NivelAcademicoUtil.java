package com.erwin.backend.util;

import com.erwin.backend.entities.Carrera;

/**
 * Deriva los niveles académicos clave del reglamento UIC-UTEQ a partir de la
 * duración (en niveles/semestres) de la malla de una carrera:
 *   - antepenúltimo nivel → orientación/tutoría DT1 (Módulo 4)
 *   - penúltimo nivel     → matrícula de Titulación I (Módulo 3)
 */
public final class NivelAcademicoUtil {

    private NivelAcademicoUtil() {}

    public static int antepenultimo(Carrera carrera) {
        return requireDuracion(carrera) - 2;
    }

    public static int penultimo(Carrera carrera) {
        return requireDuracion(carrera) - 1;
    }

    public static int ultimo(Carrera carrera) {
        return requireDuracion(carrera);
    }

    private static int requireDuracion(Carrera carrera) {
        if (carrera == null) {
            throw new IllegalStateException("El estudiante no tiene una carrera registrada");
        }
        if (carrera.getDuracionNiveles() == null) {
            throw new IllegalStateException(
                    "La carrera '" + carrera.getNombre() + "' no tiene configurada la duración de niveles "
                            + "(duracionNiveles); el Coordinador debe configurarla en el catálogo de carreras "
                            + "antes de poder validar el nivel académico del estudiante");
        }
        return carrera.getDuracionNiveles();
    }
}
