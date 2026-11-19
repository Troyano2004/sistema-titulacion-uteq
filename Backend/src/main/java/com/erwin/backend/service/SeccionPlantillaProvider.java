package com.erwin.backend.service;

import com.erwin.backend.enums.TipoTicPlantilla;

/**
 * Clasifica el tipo de trabajo (texto libre configurado por Coordinación,
 * {@code Tipotrabajotitulacion.nombre}) en una de las 4 plantillas de TIC
 * por coincidencia de palabras clave; si no reconoce ninguna, usa
 * PROYECTO_INVESTIGACION (Art. 42, la plantilla más completa) como respaldo
 * seguro.
 *
 * La estructura de secciones de cada plantilla YA NO vive aquí — se movió a
 * la tabla seccion_plantilla_tic (migración V18) y se consulta vía
 * {@link SeccionPlantillaTicService}, para que quede definida en la BD y no
 * como una constante en el código.
 */
public final class SeccionPlantillaProvider {

    private SeccionPlantillaProvider() {}

    public record SeccionDef(String codigo, String titulo, boolean obligatoria, boolean autoLlenada) {}

    public static TipoTicPlantilla clasificar(String nombreTipoTrabajo) {
        String n = nombreTipoTrabajo == null ? "" : normalizar(nombreTipoTrabajo);
        if (n.contains("TECNOLOGIC")) return TipoTicPlantilla.PROYECTO_TECNOLOGICO;
        if (n.contains("CASO")) return TipoTicPlantilla.ESTUDIO_DE_CASO;
        if (n.contains("ARTICULO") || n.contains("CIENTIFIC")) return TipoTicPlantilla.ARTICULO_CIENTIFICO;
        return TipoTicPlantilla.PROYECTO_INVESTIGACION;
    }

    private static String normalizar(String s) {
        return java.text.Normalizer.normalize(s.toUpperCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
