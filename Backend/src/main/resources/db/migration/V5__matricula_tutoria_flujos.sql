-- ============================================================================
-- V5 — Flujos de matrícula (Titulación I/II) y tutoría/orientación DT1
--
-- Soporta las validaciones reglamentarias añadidas en MatriculaTitulacionService,
-- EleccionService, CoordinadorService.asignarTutorDt1 y TutoriaListadoService:
--   - duracion_niveles en carrera: permite derivar el antepenúltimo/penúltimo
--     nivel de la malla sin hardcodear una duración fija por carrera.
--   - fecha_limite_orientacion en dt1_tutor_estudiante: plazo (semana 20)
--     para registrar la orientación DT1.
--   - documento_adjunto_id_caso_medico en matricula_titulacion: soporte del
--     cambio de opción de titulación fuera de la semana 4 por caso médico.
--
-- Todas las columnas son NULLABLE: no se pierde ni invalida ningún registro
-- existente. Solo agrega — no elimina ni renombra columnas existentes.
-- ============================================================================

ALTER TABLE carrera ADD COLUMN duracion_niveles INTEGER;

ALTER TABLE dt1_tutor_estudiante ADD COLUMN fecha_limite_orientacion DATE;

ALTER TABLE matricula_titulacion ADD COLUMN documento_adjunto_id_caso_medico BIGINT;
