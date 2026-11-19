-- ============================================================================
-- V16 — Corrige por qué no se podían editar ~15 usuarios desde el panel de
-- administración (aparecían "(sin roles)" y el modal de edición no dejaba
-- guardar hasta re-seleccionar el rol a mano cada vez).
--
-- Causa raíz: el catálogo rol_app (roles del APLICATIVO, el que alimenta el
-- panel de admin) nunca tuvo entradas para DECANO, DIRECCION_ACADEMICA y
-- SECRETARIO_ABOGADO, aunque esos 3 roles sí existen en roles_sistema (rol
-- BASE) y se usan activamente en @PreAuthorize por todo el backend. Como
-- consecuencia, ningún usuario con esos roles (ni los que se les asignó vía
-- el campo legado usuario.rol_asignado en vez de la tabla relacional
-- usuario_rol_app) tenía una fila correspondiente en usuario_rol_app, y
-- vw_usuario_admin_v3 (la vista que alimenta /admin/usuarios) los mostraba
-- sin ningún rol resuelto.
-- ============================================================================

-- 1) Completa el catálogo rol_app con los 3 roles institucionales que faltaban.
INSERT INTO rol_app (nombre, descripcion, activo, id_rol_base)
SELECT 'ROLE_DECANO', 'Decano de facultad', true, rs.id_rol
FROM roles_sistema rs
WHERE rs.nombre_rol = 'DECANO'
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol_app (nombre, descripcion, activo, id_rol_base)
SELECT 'ROLE_DIRECCION_ACADEMICA', 'Dirección académica', true, rs.id_rol
FROM roles_sistema rs
WHERE rs.nombre_rol = 'DIRECCION_ACADEMICA'
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO rol_app (nombre, descripcion, activo, id_rol_base)
SELECT 'ROLE_SECRETARIO_ABOGADO', 'Secretario abogado', true, rs.id_rol
FROM roles_sistema rs
WHERE rs.nombre_rol = 'SECRETARIO_ABOGADO'
ON CONFLICT (nombre) DO NOTHING;

-- 2) Backfill: usuarios cuyo rol solo vive en el texto legado
--    usuario.rol_asignado y nunca se migró a la tabla relacional
--    usuario_rol_app (por eso el login seguía funcionando —AuthService cae a
--    rol_asignado como fallback— pero el panel de administración, que
--    depende 100% de usuario_rol_app, los mostraba sin rol).
INSERT INTO usuario_rol_app (id_usuario, id_rol_app)
SELECT u.id_usuario, ra.id_rol_app
FROM usuario u
JOIN rol_app ra ON ra.nombre = 'ROLE_' || u.rol_asignado
WHERE u.rol_asignado IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM usuario_rol_app x WHERE x.id_usuario = u.id_usuario)
ON CONFLICT (id_usuario, id_rol_app) DO NOTHING;
