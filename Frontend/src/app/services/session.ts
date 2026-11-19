export interface SessionUser {
  rol?:   string;
  roles?: string[];   // ✅ todos los roles del usuario
  [key: string]: unknown;
}

const SESSION_KEY = 'usuario';

export const setSessionUser = (user: SessionUser): void => {
  localStorage.setItem(SESSION_KEY, JSON.stringify(user));
};

export const getSessionUser = (): SessionUser | null => {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as SessionUser;
  } catch {
    localStorage.removeItem(SESSION_KEY);
    return null;
  }
};

export const isAuthenticated = (): boolean => !!getSessionUser();

export const normalizeRole = (rol: unknown): string =>
  String(rol || '').trim().toUpperCase();

export const hasRole = (rol: unknown, ...expectedRoles: string[]): boolean => {
  const normalizedRole = normalizeRole(rol);
  return expectedRoles.map(item => normalizeRole(item)).includes(normalizedRole);
};

// ✅ Devuelve todos los roles del usuario como array normalizado
// Fix: solo usa el array "roles" si tiene elementos con contenido real
export const getUserRoles = (): string[] => {
  const user = getSessionUser();
  if (!user) return [];

  // Usa el array "roles" solo si tiene elementos no vacíos
  if (Array.isArray(user.roles)) {
    const fromArray = user.roles
      .map(r => normalizeRole(r))
      .filter(r => r.length > 0);

    if (fromArray.length > 0) {
      return fromArray;
    }
  }

  // Fallback: usar el campo "rol" simple
  const single = normalizeRole(user.rol);
  return single ? [single] : [];
};

// ✅ Verifica si el usuario tiene AL MENOS UNO de los roles indicados
export const hasAnyRole = (...expectedRoles: string[]): boolean => {
  const userRoles = getUserRoles();
  const normalized = expectedRoles.map(r => normalizeRole(r));
  return userRoles.some(r => normalized.includes(r));
};

export const getSessionEntityId = (
  user: SessionUser | null,
  kind: 'estudiante' | 'docente'
): number | null => {
  if (!user) return null;
  const keys =
    kind === 'estudiante'
      ? ['idEstudiante', 'estudianteId', 'id_usuario', 'idUsuario']
      : ['idDocente', 'docenteId', 'id_usuario', 'idUsuario'];

  for (const key of keys) {
    const raw    = user[key];
    const parsed = Number(raw);
    if (Number.isFinite(parsed) && parsed > 0) return parsed;
  }
  return null;
};

export const getRoleHomeRoute = (rol: unknown): string | null => {
  const userRoles = getUserRoles();

  // Usa el primer rol disponible para determinar si hay sesión válida.
  // Todos los roles aterrizan en el menú principal (/app/inicio): desde
  // ahí cada quien navega, con las cajas del menú, a lo que su rol permite
  // ver (incluida Administración para ADMIN, con un clic).
  const primaryRole = userRoles.length > 0 ? userRoles[0] : normalizeRole(rol);

  const rolesValidos = [
    'ROLE_ADMIN', 'ROLE_COORDINADOR', 'ROLE_DOCENTE', 'ROLE_DOCENTE_TITULADO',
    'ROLE_ESTUDIANTE', 'ROLE_DECANO', 'ROLE_SECRETARIO_ABOGADO',
    'ROLE_DIRECCION_ACADEMICA', 'ROLE_GESTOR_USUARIOS', 'ROLE_ROL_REPORT',
  ];
  if (primaryRole && rolesValidos.includes(primaryRole)) return '/app/inicio';

  return null;
};
