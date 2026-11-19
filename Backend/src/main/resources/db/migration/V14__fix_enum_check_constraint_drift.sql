-- Corrige CHECK constraints que quedaron desincronizados de sus enums Java
-- (se agregaron valores al enum sin actualizar el constraint de BD, lo que
-- provoca un 500 al intentar persistir esos estados válidos en la aplicación).

-- banco_temas.estado: al enum EstadoBancoTemas se le agregaron APROBADO_COMISION
-- y APROBADO_CONSEJO_DIRECTIVO (flujo de aprobación por Comisión + Consejo
-- Directivo) pero el constraint original solo permitía los 4 valores iniciales.
ALTER TABLE banco_temas DROP CONSTRAINT banco_temas_estado_check;
ALTER TABLE banco_temas ADD CONSTRAINT banco_temas_estado_check
    CHECK (estado IN ('PROPUESTO', 'SUGERIDO', 'RECHAZADO', 'APROBADO',
                       'APROBADO_COMISION', 'APROBADO_CONSEJO_DIRECTIVO'));

-- documento_titulacion.estado: el enum EstadoDocumento tiene los estados del
-- flujo de tribunal/sustentación (LISTO_PARA_TRIBUNAL, TRIBUNAL_ASIGNADO,
-- SUSTENTACION_AGENDADA, SUSTENTADO, CERRADO_APROBADO, CERRADO_REPROBADO) que
-- nunca se agregaron al constraint. Se quita también 'DESARROLLO', que no
-- existe en el enum y no tiene ninguna fila usándolo (verificado antes de
-- esta migración).
ALTER TABLE documento_titulacion DROP CONSTRAINT documento_titulacion_estado_check;
ALTER TABLE documento_titulacion ADD CONSTRAINT documento_titulacion_estado_check
    CHECK (estado IN ('BORRADOR', 'EN_REVISION', 'CORRECCION_REQUERIDA',
                       'APROBADO_POR_DIRECTOR', 'LISTO_PARA_TRIBUNAL', 'TRIBUNAL_ASIGNADO',
                       'SUSTENTACION_AGENDADA', 'SUSTENTADO', 'CERRADO_APROBADO',
                       'CERRADO_REPROBADO', 'ANTIPLAGIO_APROBADO', 'EN_PREDEFENSA',
                       'LISTO_SUSTENTACION'));
