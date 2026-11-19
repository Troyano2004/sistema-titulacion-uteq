-- ============================================================================
-- Seed de roles institucionales — Reglamento UIC-UTEQ
-- Para una base de datos nueva (sin datos previos que preservar).
-- Inserta los roles del sistema si no existen todavía.
-- ============================================================================

INSERT INTO roles_sistema (nombre_rol)
SELECT v.nombre_rol
FROM (VALUES
    ('ADMIN'),
    ('ESTUDIANTE'),
    ('DOCENTE'),
    ('DOCENTE_TITULADO'),
    ('COORDINADOR'),
    ('DECANO'),
    ('SECRETARIO_ABOGADO'),
    ('DIRECCION_ACADEMICA'),
    ('GESTOR_USUARIOS'),
    ('ROL_REPORT')
) AS v(nombre_rol)
WHERE NOT EXISTS (
    SELECT 1 FROM roles_sistema r WHERE r.nombre_rol = v.nombre_rol
);
