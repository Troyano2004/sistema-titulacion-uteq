

import { ApplicationConfig, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import {
  provideHttpClient,
  withInterceptors
} from '@angular/common/http';
import { provideQuillConfig } from 'ngx-quill';
import { LucideAngularModule } from 'lucide-angular';

import { routes } from './app.routes';
import { authInterceptor } from './interceptor/auth.interceptor';
import { APP_ICONS } from './layout/icons';

export const appConfig: ApplicationConfig = {
  providers: [
    // Esta app se venía ejecutando sin zone.js (modo zoneless implícito, sin
    // provideZoneChangeDetection ni provideExperimentalZonelessChangeDetection),
    // por lo que cualquier .subscribe(v => this.campo = v) fuera de un signal
    // o de un evento de plantilla nunca disparaba un re-render — así se
    // encontraron varias pantallas (dashboard, listados) que cargaban los
    // datos pero se quedaban en "Cargando...". Se reincorpora zone.js para
    // volver al modelo de change detection con el que el resto de la app
    // fue construida, sin tener que parchear cada componente uno por uno.
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),

    // ✅ FIX: el interceptor adjunta Authorization: Bearer <token> en cada request.
    // El DbSessionFilter del backend lee el token como PRIORIDAD 1 y cambia la
    // conexión al usuario BD del usuario logueado (ej: app_docente1, app_admin...).
    // Sin esto, el backend siempre usa auth_reader (conexión por defecto).
    provideHttpClient(
      withInterceptors([authInterceptor])
    ),

    // Editor de texto enriquecido (reemplaza el editor execCommand anterior)
    provideQuillConfig({
      modules: {
        toolbar: [
          ['bold', 'italic', 'underline', 'strike'],
          [{ list: 'ordered' }, { list: 'bullet' }],
          [{ header: [1, 2, 3, false] }],
          [{ align: [] }],
          ['link', 'image'],
          ['clean'],
        ],
      },
      theme: 'snow',
    }),

    // Set de íconos SVG (Lucide) usado en el menú principal y la topbar —
    // ver layout/icons.ts. Registrado una sola vez aquí; cada componente
    // solo necesita importar LucideAngularModule para usarlos.
    importProvidersFrom(LucideAngularModule.pick(APP_ICONS)),
  ],
};
