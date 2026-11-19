package com.erwin.backend.enums;

/**
 * Clasificación usada solo para elegir la plantilla de secciones al crear un
 * DocumentoTIC (TAREA 1.2) — se deriva del nombre de
 * {@code Tipotrabajotitulacion} (catálogo de texto libre configurado por
 * Coordinación), no se persiste como columna propia.
 */
public enum TipoTicPlantilla {
    PROYECTO_INVESTIGACION,
    PROYECTO_TECNOLOGICO,
    ESTUDIO_DE_CASO,
    ARTICULO_CIENTIFICO
}
