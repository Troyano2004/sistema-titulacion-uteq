-- ============================================================================
-- V7 — Desarrollo del TIC, predefensa/defensa y nota de grado
--      (Módulos 8, 9 y 17)
--
-- Soporta:
--   - Bloqueo real del cierre de corte sin 5 asesorías / reuniones mensuales
--     / plazo vencido (Dt2Service.cerrarCorte) — usa las fechas de corte del
--     periodo, ya que ActaCorte no existe todavía cuando se valida el cierre.
--   - Rúbrica 20/20/30/30 en predefensa (reutiliza las columnas existentes
--     de evaluacion_sustentacion: calidad_trabajo/originalidad/dominio_tema/
--     preguntas — no se agregan columnas nuevas para esto).
--   - Calificaciones inapelables una vez que el panel completo calificó
--     (Art. 65).
--   - Registro formal de la nota de predefensa como examen final de
--     Desarrollo de Titulación II.
--   - Resultado COMPILATIO por estudianteId + certificado antiplagio.
--
-- Todas las columnas son NULLABLE o tienen DEFAULT: no se pierde ni invalida
-- ningún registro existente.
-- ============================================================================

ALTER TABLE evaluacion_sustentacion ADD COLUMN calificacion_finalizada BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE periodo_titulacion ADD COLUMN fecha_inicio_corte1 DATE;
ALTER TABLE periodo_titulacion ADD COLUMN fecha_fin_corte1    DATE;
ALTER TABLE periodo_titulacion ADD COLUMN fecha_inicio_corte2 DATE;
ALTER TABLE periodo_titulacion ADD COLUMN fecha_fin_corte2    DATE;

ALTER TABLE antiplagio_intento ALTER COLUMN url_informe DROP NOT NULL;
ALTER TABLE antiplagio_intento ADD COLUMN archivo_reporte_id         BIGINT;
ALTER TABLE antiplagio_intento ADD COLUMN certificado_emitido        BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE antiplagio_intento ADD COLUMN fecha_emision_certificado  TIMESTAMP;

ALTER TABLE proyecto_titulacion ADD COLUMN nota_predefensa_final NUMERIC(4,2);
