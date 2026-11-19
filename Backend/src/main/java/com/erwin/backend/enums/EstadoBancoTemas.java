package com.erwin.backend.enums;

public enum EstadoBancoTemas {
    PROPUESTO,
    SUGERIDO,
    RECHAZADO,
    APROBADO,
    /** Aprobado por la Comisión de Investigación Formativa; aún no disponible para estudiantes. */
    APROBADO_COMISION,
    /** Aprobado por el Consejo Directivo de Facultad; único estado visible/seleccionable por estudiantes. */
    APROBADO_CONSEJO_DIRECTIVO
}
