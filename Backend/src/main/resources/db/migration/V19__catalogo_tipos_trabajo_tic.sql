-- ============================================================================
-- V19 — Siembra en tipo_trabajo_titulacion las 4 modalidades de TIC que
-- definen qué estructura de documento le toca a cada estudiante (Art.
-- 42/43/44/45). Esta tabla es el punto de partida real: un estudiante (o
-- quien registre su documento) elige aquí su tipo de trabajo, y de ese
-- nombre se deriva la plantilla de secciones (SeccionPlantillaProvider.
-- clasificar() + seccion_plantilla_tic, ver migración V18).
--
-- Antes de esta migración solo existía la fila "Proyecto Tecnológico"
-- (probablemente cargada a mano en algún momento de pruebas) — las otras 3
-- modalidades nunca se sembraron, así que no aparecían en ningún selector
-- del sistema aunque su plantilla de secciones ya estuviera definida.
--
-- Todas quedan bajo la modalidad "Trabajo de Integración Curricular"
-- (id_modalidad=1): Examen Complexivo es una modalidad aparte que no pasa
-- por este catálogo ni por DocumentoTIC.
-- ============================================================================

INSERT INTO tipo_trabajo_titulacion (nombre, id_modalidad)
SELECT 'Proyecto de Investigación', m.id_modalidad
FROM modalidad_titulacion m
WHERE m.nombre = 'Trabajo de Integración Curricular'
  AND NOT EXISTS (SELECT 1 FROM tipo_trabajo_titulacion WHERE nombre = 'Proyecto de Investigación');

INSERT INTO tipo_trabajo_titulacion (nombre, id_modalidad)
SELECT 'Estudio de Caso', m.id_modalidad
FROM modalidad_titulacion m
WHERE m.nombre = 'Trabajo de Integración Curricular'
  AND NOT EXISTS (SELECT 1 FROM tipo_trabajo_titulacion WHERE nombre = 'Estudio de Caso');

INSERT INTO tipo_trabajo_titulacion (nombre, id_modalidad)
SELECT 'Artículo Científico', m.id_modalidad
FROM modalidad_titulacion m
WHERE m.nombre = 'Trabajo de Integración Curricular'
  AND NOT EXISTS (SELECT 1 FROM tipo_trabajo_titulacion WHERE nombre = 'Artículo Científico');
