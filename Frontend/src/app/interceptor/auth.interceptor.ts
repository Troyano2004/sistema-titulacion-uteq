import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Interceptor que adjunta el JWT (usuario + roles, sin credenciales de BD)
 * en el header Authorization de cada request al backend.
 *
 * El DbSessionFilter del backend usa el usuario del token como PRIORIDAD 1
 * para resolver en servidor la conexión al usuario de BD correcto — la
 * contraseña de BD ya no viaja dentro del token.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const raw = localStorage.getItem('usuario');
  if (!raw) return next(req);

  try {
    const user = JSON.parse(raw);
    const token = user?.token;

    if (token) {
      const cloned = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
      return next(cloned);
    }
  } catch {
    // JSON inválido, ignorar
  }

  return next(req);
};
