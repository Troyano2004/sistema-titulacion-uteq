-- ============================================================================
-- V6 — Propuesta del TIC (Módulo 6) y cierre del Banco de Temas (Módulo 5)
--
-- Soporta:
--   - Aprobación del Consejo Directivo de Facultad sobre un tema del banco
--     (BancoTemasConsejoService) — nuevos estados APROBADO_COMISION /
--     APROBADO_CONSEJO_DIRECTIVO ya están en el enum Java, aquí solo se
--     amplía el ancho de la columna y se agregan los campos de auditoría.
--   - Modalidad de entrega de la propuesta configurable por el docente DT1
--     (ConfiguracionTitulacionService).
--   - Control de plazos y resolución del Consejo Directivo sobre la
--     propuesta (PropuestaResolucionService): recepción de Comisión,
--     designación de Director/Tribunal.
--   - Flujo de modificación post-aprobación del anteproyecto
--     (AnteproyectoService.solicitarModificacion/aprobarModificacion).
--
-- Todas las columnas son NULLABLE (o tienen DEFAULT) y no se elimina ni
-- renombra nada existente.
-- ============================================================================

ALTER TABLE banco_temas ALTER COLUMN estado TYPE VARCHAR(30);
ALTER TABLE banco_temas ADD COLUMN id_usuario_aprobo_consejo INTEGER;
ALTER TABLE banco_temas ADD COLUMN fecha_aprobacion_consejo  DATE;
ALTER TABLE banco_temas ADD CONSTRAINT fk_bancotemas_usuario_consejo FOREIGN KEY (id_usuario_aprobo_consejo) REFERENCES usuario (id_usuario);

ALTER TABLE dt1_asignacion ADD COLUMN modalidad_entrega VARCHAR(20);

ALTER TABLE propuesta_titulacion ADD COLUMN fecha_recepcion_comision            TIMESTAMP;
ALTER TABLE propuesta_titulacion ADD COLUMN resolucion_consejo_directivo        BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE propuesta_titulacion ADD COLUMN fecha_resolucion_consejo_directivo  TIMESTAMP;
ALTER TABLE propuesta_titulacion ADD COLUMN id_usuario_resolucion_consejo       INTEGER;
ALTER TABLE propuesta_titulacion ADD CONSTRAINT fk_propuesta_usuario_consejo FOREIGN KEY (id_usuario_resolucion_consejo) REFERENCES usuario (id_usuario);

ALTER TABLE anteproyecto_titulacion ADD COLUMN motivo_modificacion_solicitada     TEXT;
ALTER TABLE anteproyecto_titulacion ADD COLUMN campos_a_modificar_solicitados     TEXT;
ALTER TABLE anteproyecto_titulacion ADD COLUMN id_docente_solicito_modificacion   INTEGER;
ALTER TABLE anteproyecto_titulacion ADD CONSTRAINT fk_ante_docente_solicito_modificacion FOREIGN KEY (id_docente_solicito_modificacion) REFERENCES docente (id_docente);
