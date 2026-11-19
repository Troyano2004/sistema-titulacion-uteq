import { Injectable, signal } from '@angular/core';
import { ComisionTemasService } from '../services/comision-temas';
import { getSessionEntityId, getSessionUser, getUserRoles } from '../services/session';
import { AppRole, MenuItem, MenuSection } from './menu.model';

/**
 * Fuente única de la navegación de la app (antes vivía solo dentro de
 * ShellComponent). Se comparte entre el shell (topbar/buscador/breadcrumb)
 * y las páginas de menú (/app/inicio, /app/menu/:slug) para que ambos
 * lean exactamente los mismos datos y reglas de visibilidad.
 *
 * visibleSections es un signal (no un campo plano actualizado en un
 * .subscribe) a propósito: esta app corre zoneless (sin zone.js), y un
 * signal es lo único que dispara re-render automáticamente ahí cuando
 * el valor cambia de forma asíncrona (p.ej. al resolver la modalidad
 * del estudiante).
 */
@Injectable({ providedIn: 'root' })
export class MenuService {
  private readonly ALL_SECTIONS: MenuSection[] = [
    {
      title: 'Dashboard',
      icon: 'layout-dashboard',
      description: 'Resumen general de tu actividad',
      roles: ['ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'ESTUDIANTE', 'COORDINADOR', 'DECANO', 'SECRETARIO_ABOGADO', 'DIRECCION_ACADEMICA'],
      items: [
        { label: 'Resumen general', path: '/app/dashboard', roles: ['ADMIN', 'COORDINADOR', 'DECANO', 'DIRECCION_ACADEMICA'] },
        { label: 'Mi panel docente', path: '/app/dashboard', roles: ['DOCENTE', 'DOCENTE_TITULADO'] },
        { label: 'Mi panel', path: '/app/dashboard', roles: ['ESTUDIANTE', 'SECRETARIO_ABOGADO'] },
      ],
    },
    {
      title: 'Catálogos Académicos',
      icon: 'graduation-cap',
      description: 'Universidad, facultades, carreras y periodos',
      roles: ['ADMIN', 'COORDINADOR'],
      items: [
        { label: 'Universidad', path: '/app/catalogos/universidad' },
        { label: 'Facultad', path: '/app/catalogos/facultad' },
        { label: 'Docentes y Carreras', path: '/app/catalogos/docentes' },
        { label: 'Carrera', path: '/app/catalogos/carrera' },
        { label: 'Modalidad Titulación', path: '/app/catalogos/modalidad' },
        { label: 'Período Académico', path: '/app/catalogos/periodo' },
        { label: 'Tipo Trabajo Titulación', path: '/app/catalogos/tipo-trabajo' },
        { label: 'Carrera-Modalidad', path: '/app/catalogos/carrera-modalidad' },
      ],
    },
    {
      title: 'Banco de Temas',
      icon: 'book-open',
      description: 'Temas disponibles para trabajos de titulación',
      roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN', 'COORDINADOR', 'ESTUDIANTE'],
      items: [
        { label: 'Listado de temas', path: '/app/temas', roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN', 'COORDINADOR'] },
        { label: 'Registrar tema', path: '/app/temas/nuevo', roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN', 'COORDINADOR'] },
        { label: 'Aprobación temas', path: '/app/temas/aprobacion', roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN', 'COORDINADOR'] },
        { label: 'Mis propuestas de tema', path: '/app/temas/mis-propuestas', roles: ['ESTUDIANTE'] },
      ],
    },
    {
      title: 'Propuesta y Anteproyecto',
      icon: 'file-pen-line',
      description: 'Registro, revisión y aprobación de propuestas',
      roles: ['ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'ESTUDIANTE'],
      items: [
        { label: 'Propuestas pendientes', path: '/app/propuesta/pendientes', roles: ['ADMIN', 'DOCENTE', 'DOCENTE_TITULADO'] },
        { label: 'Registrar propuesta', path: '/app/propuesta/nueva', roles: ['ESTUDIANTE', 'ADMIN'] },
        { label: 'Registrar anteproyecto', path: '/app/anteproyecto/nuevo', roles: ['ESTUDIANTE', 'ADMIN'] },
        { label: 'Revisión por director', path: '/app/propuesta/revision', roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN'] },
        { label: 'Historial observaciones', path: '/app/tutorias/historial', roles: ['ESTUDIANTE', 'ADMIN'] },
        { label: 'Historial observaciones', path: '/app/propuesta/historial', roles: ['DOCENTE', 'DOCENTE_TITULADO'] },
        { label: 'Reporte de propuestas', path: '/app/propuesta/reporte', roles: ['ADMIN', 'COORDINADOR', 'DOCENTE', 'DOCENTE_TITULADO'] },
      ],
    },
    {
      title: 'Tutorías y Dirección',
      icon: 'users',
      description: 'Seguimiento de anteproyectos y tutorías',
      roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN'],
      items: [
        { label: 'Mis anteproyectos', path: '/app/director/mis-anteproyectos' },
        { label: 'Tutorías', path: '/app/director/tutorias' },
        { label: 'Acta de revisión', path: '/app/director/acta' },
        { label: 'Revisión Final Anteproyecto', path: '/app/dt1/lista' },
        { label: 'Registrar tutoría', path: '/app/tutorias/nueva' },
        { label: 'Actas de tutoría', path: '/app/tutorias/actas' },
        { label: 'Historial', path: '/app/tutorias/historial', roles: ['ADMIN'] },
      ],
    },
    {
      title: 'Proyecto de Titulación',
      icon: 'file-text',
      description: 'Documento, revisión y estado del proyecto',
      roles: ['ADMIN', 'DOCENTE', 'DOCENTE_TITULADO'],
      items: [
        { label: 'Documento por secciones', path: '/app/proyecto/documento' },
        { label: 'Revisión por secciones', path: '/app/proyecto/revision' },
        { label: 'Correcciones', path: '/app/proyecto/correcciones' },
        { label: 'Estado del proyecto', path: '/app/proyecto/estado' },
      ],
    },
    {
      title: 'Titulación II',
      icon: 'clipboard-check',
      description: 'Workflow, predefensa, sustentación y seguimiento',
      roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ESTUDIANTE', 'ADMIN'],
      items: [
        { label: 'Documento de titulación', path: '/app/titulacion2/documento', roles: ['ESTUDIANTE'], soloNoComplexivo: true },
        { label: 'Editor del TIC por secciones', path: '/app/titulacion2/documento-tic', roles: ['ESTUDIANTE'], soloNoComplexivo: true },
        { label: 'Bibliografía', path: '/app/titulacion2/bibliografia', roles: ['ESTUDIANTE'], soloNoComplexivo: true },
        { label: 'Documentos pendientes', path: '/app/titulacion2/revisar', roles: ['DOCENTE', 'DOCENTE_TITULADO'] },
        { label: 'Seguimiento DT2', path: '/app/director/seguimiento-dt2', roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN'] },
        { label: 'Antiplagio COMPILATIO', path: '/app/director/antiplagio-dt2', roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN'] },
        { label: 'Predefensa', path: '/app/titulacion2/predefensa', roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN'] },
        { label: 'Sustentación Final', path: '/app/titulacion2/sustentacion', roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN'] },
        { label: 'Complexivo - Informes', path: '/app/titulacion2/complexivo-informes', roles: ['DOCENTE', 'DOCENTE_TITULADO', 'ADMIN'] },
      ],
    },
    {
      title: 'Titulación II — Complexivo',
      icon: 'clipboard-list',
      description: 'Informe práctico y examen complexivo',
      roles: ['ESTUDIANTE'],
      soloComplexivo: true,
      items: [
        { label: 'Informe Práctico', path: '/app/complexivo/informe', roles: ['ESTUDIANTE'] },
        { label: 'Examen Complexivo', path: '/app/complexivo-real/rendir', roles: ['ESTUDIANTE'] },
      ],
    },
    {
      title: 'Examen Complexivo — Banco de Preguntas',
      icon: 'circle-question-mark',
      description: 'Gestión del banco de preguntas',
      roles: ['DOCENTE', 'DOCENTE_TITULADO', 'COORDINADOR', 'ADMIN'],
      items: [
        { label: 'Banco de preguntas', path: '/app/complexivo-real/banco-preguntas' },
      ],
    },
    {
      title: 'Documentos',
      icon: 'folder-open',
      description: 'Habilitantes, versiones y expediente',
      roles: ['ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'ESTUDIANTE', 'SECRETARIO_ABOGADO'],
      items: [
        { label: 'Habilitantes', path: '/app/documentos/habilitantes' },
        { label: 'Versiones', path: '/app/documentos/versiones' },
        { label: 'Expediente', path: '/app/documentos/expediente' },
      ],
    },
    {
      title: 'Legalización',
      icon: 'scale',
      description: 'Validación legal y aprobación final',
      roles: ['ADMIN', 'COORDINADOR', 'DECANO', 'SECRETARIO_ABOGADO'],
      items: [
        { label: 'Validación legal', path: '/app/legalizacion/validacion' },
        { label: 'Checklist', path: '/app/legalizacion/checklist' },
        { label: 'Aprobación final', path: '/app/legalizacion/aprobacion' },
      ],
    },
    {
      title: 'Reportes',
      icon: 'chart-column',
      description: 'Reportes institucionales y de proceso',
      roles: ['ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'COORDINADOR', 'DECANO', 'DIRECCION_ACADEMICA', 'ROL_REPORT'],
      items: [
        { label: 'Expediente por estudiante', path: '/app/reportes/expediente' },
        { label: 'Por período', path: '/app/reportes/periodo' },
        { label: 'Actas y constancias', path: '/app/reportes/actas' },
        { label: 'Dashboard institucional', path: '/app/reportes/dashboard-institucional', roles: ['ADMIN', 'COORDINADOR', 'DIRECCION_ACADEMICA'] },
        { label: 'Trazabilidad de estudiante', path: '/app/reportes/trazabilidad', roles: ['ADMIN', 'COORDINADOR'] },
      ],
    },
    {
      title: 'Calendario de Titulación',
      icon: 'calendar-days',
      description: 'Fechas y plazos del proceso de titulación',
      roles: ['ADMIN', 'COORDINADOR', 'DECANO', 'DIRECCION_ACADEMICA', 'DOCENTE', 'DOCENTE_TITULADO'],
      items: [
        { label: 'Ver calendario', path: '/app/calendario-titulacion' },
      ],
    },
    {
      title: 'Expediente Digital',
      icon: 'archive',
      description: 'Expediente y documentos habilitantes del estudiante',
      roles: ['ESTUDIANTE', 'ADMIN', 'COORDINADOR', 'SECRETARIO_ABOGADO', 'DECANO'],
      items: [
        { label: 'Mi expediente', path: '/app/expediente/mi-expediente', roles: ['ESTUDIANTE', 'ADMIN'] },
        { label: 'Validación de expedientes', path: '/app/expediente/validacion', roles: ['ADMIN', 'COORDINADOR', 'SECRETARIO_ABOGADO', 'DECANO'] },
      ],
    },
    {
      title: 'Incorporación y SGA',
      icon: 'award',
      description: 'Incorporación de títulos e importación de datos',
      roles: ['ADMIN', 'SECRETARIO_ABOGADO', 'DIRECCION_ACADEMICA', 'DECANO', 'COORDINADOR'],
      items: [
        { label: 'Incorporación de títulos', path: '/app/incorporacion/panel', roles: ['ADMIN', 'SECRETARIO_ABOGADO', 'DIRECCION_ACADEMICA', 'DECANO'] },
        { label: 'Importación de datos del SGA', path: '/app/sga/importacion', roles: ['ADMIN', 'COORDINADOR'] },
      ],
    },
    {
      title: 'Coordinación',
      icon: 'compass',
      description: 'Gestión y seguimiento de la coordinación de titulación',
      roles: ['COORDINADOR', 'ADMIN'],
      items: [
        { label: 'Seguimiento de proyectos', path: '/app/coordinador/seguimiento' },
        { label: 'Control de directores', path: '/app/coordinador/directores' },
        { label: 'Validación administrativa', path: '/app/coordinador/validacion' },
        { label: 'Control de tutorías', path: '/app/coordinador/tutorias' },
        { label: 'Observaciones administrativas', path: '/app/coordinador/observaciones' },
        { label: 'DT2 - Configuración Inicial', path: '/app/coordinador/configuracion-dt2' },
        { label: 'DT2 - Predefensa', path: '/app/titulacion2/predefensa' },
        { label: 'DT2 - Sustentación Final', path: '/app/titulacion2/sustentacion' },
        { label: 'Reportes', path: '/app/coordinador/reportes' },
        { label: 'Comisión formativa', path: '/app/coordinador/comision' },
        { label: 'DT1 - Docentes y Tutores', path: '/app/coordinador/dt1-asignacion' },
        { label: 'Complexivo - Asignar DT1', path: '/app/coordinador/complexivo-asignacion' },
        { label: 'Complexivo - Asignar DT2', path: '/app/coordinador/complexivo-dt2-asignacion' },
        { label: 'Examen Complexivo - Gestión', path: '/app/director-admin/examen-complexivo' },
      ],
    },
    {
      title: 'Dirección Académica',
      icon: 'landmark',
      description: 'Gestión institucional y aprobación de reportes',
      roles: ['DIRECCION_ACADEMICA', 'DECANO', 'ADMIN'],
      items: [
        { label: 'Gestión institucional', path: '/app/director-admin/gestion' },
        { label: 'Aprobación de reportes', path: '/app/director-admin/reportes' },
        { label: 'Examen Complexivo - Gestión', path: '/app/director-admin/examen-complexivo' },
      ],
    },
    {
      title: 'Secretaría y Asesoría Legal',
      icon: 'gavel',
      description: 'Actas, documentos y validación jurídica',
      roles: ['SECRETARIO_ABOGADO', 'ADMIN'],
      items: [
        { label: 'Registro de actas', path: '/app/secretaria/actas' },
        { label: 'Gestión documental', path: '/app/secretaria/documentos' },
        { label: 'Legalización', path: '/app/legalizacion/validacion' },
        { label: 'Validación jurídica', path: '/app/legal/validacion' },
      ],
    },
    {
      title: 'Administración del aplicativo',
      icon: 'settings',
      description: 'Usuarios, roles, parámetros y auditoría',
      roles: ['ADMIN', 'GESTOR_USUARIOS'],
      items: [
        { label: 'Usuarios', path: '/app/admin/usuarios' },
        { label: 'Roles y permisos', path: '/app/admin/roles' },
        { label: 'Parámetros', path: '/app/admin/parametros' },
        { label: 'Gestión de solicitudes', path: '/app/admin/gestion-solicitudes' },
        { label: 'Configuración de correo', path: '/app/admin/configuracion-correo' },
        { label: 'Dashboard Auditoría', path: '/app/admin/auditoria/dashboard' },
        { label: 'Logs de Auditoría', path: '/app/admin/auditoria/logs' },
        { label: 'Config. Auditoría', path: '/app/admin/auditoria/config' },
        { label: 'Respaldos BD', path: '/app/admin/backup', roles: ['ADMIN'] },
        { label: 'Restaurar BD', path: '/app/admin/backup/restaurar', roles: ['ADMIN'] },
      ],
    },
  ];

  private modalidadEstudiante: string | null = null;
  private static readonly ACTIVE_ROLE_KEY = 'activeRoleFilter';

  /** Secciones visibles para el usuario logueado, ya filtradas por rol y modalidad. */
  readonly visibleSections = signal<MenuSection[]>([]);

  /**
   * Rol que el usuario eligió para filtrar el menú (selector en la topbar).
   * null = "Todos mis roles": se muestra la unión de todo lo que sus roles
   * permiten ver (comportamiento de siempre). Un usuario con varios roles
   * (ej. ADMIN + DOCENTE) puede elegir ver el menú como si solo tuviera uno.
   */
  readonly activeRole = signal<AppRole | null>(null);

  constructor(private readonly comisionTemasService: ComisionTemasService) {
    this.activeRole.set(this.leerRolActivoGuardado());
    this.visibleSections.set(this.buildMenuByRoles());
    this.cargarModalidadSiEsEstudiante();
  }

  /** Todas las secciones sin filtrar por rol — usado solo por el buscador, igual que antes. */
  getAllSections(): MenuSection[] {
    return this.ALL_SECTIONS;
  }

  /** Roles reales que tiene el usuario logueado (para armar el selector). */
  misRoles(): AppRole[] {
    return this.getNormalizedRoles();
  }

  setActiveRole(role: AppRole | null): void {
    this.activeRole.set(role);
    try {
      if (role) localStorage.setItem(MenuService.ACTIVE_ROLE_KEY, role);
      else localStorage.removeItem(MenuService.ACTIVE_ROLE_KEY);
    } catch { /* localStorage no disponible (modo privado, etc.) — no bloquea el filtro */ }
    this.visibleSections.set(this.buildMenuByRoles());
  }

  private leerRolActivoGuardado(): AppRole | null {
    try {
      const guardado = localStorage.getItem(MenuService.ACTIVE_ROLE_KEY) as AppRole | null;
      // Solo lo respeta si el usuario sigue teniendo ese rol hoy (evita que
      // quede "pegado" un rol de una sesión anterior con otros roles).
      return guardado && this.getNormalizedRoles().includes(guardado) ? guardado : null;
    } catch {
      return null;
    }
  }

  getSectionBySlug(slug: string): MenuSection | undefined {
    return this.visibleSections().find(s => this.slugify(s.title) === slug);
  }

  slugify(title: string): string {
    return title
      .normalize('NFD').replace(/[̀-ͯ]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  }

  /** Busca en qué sección/item visible vive una ruta, para armar el breadcrumb. */
  findBreadcrumb(path: string): { section: MenuSection; item: MenuItem } | undefined {
    for (const section of this.visibleSections()) {
      const item = section.items.find(it => it.path === path);
      if (item) return { section, item };
    }
    return undefined;
  }

  esComplexivo(): boolean {
    return (this.modalidadEstudiante ?? '').toLowerCase().includes('complexivo');
  }

  private cargarModalidadSiEsEstudiante(): void {
    const roles = getUserRoles().map(r => r.replace('ROLE_', '').toUpperCase());
    if (!roles.includes('ESTUDIANTE')) return;

    const user = getSessionUser();
    const idEst = getSessionEntityId(user, 'estudiante');
    if (!idEst) return;

    this.comisionTemasService.obtenerEstadoModalidad(idEst).subscribe({
      next: (estado) => {
        if (estado.tieneModalidad && estado.modalidad) {
          this.modalidadEstudiante = estado.modalidad;
          this.visibleSections.set(this.buildMenuByRoles());
        }
      },
      error: () => {},
    });
  }

  private buildMenuByRoles(): MenuSection[] {
    const seleccionado = this.activeRole();
    const userRoles = seleccionado ? [seleccionado] : this.getNormalizedRoles();
    if (userRoles.length === 0) return [];

    return this.ALL_SECTIONS
      .filter(sec => {
        if (sec.roles && !sec.roles.some(r => userRoles.includes(r))) return false;
        if (sec.soloComplexivo && !this.esComplexivo()) return false;
        if (sec.soloNoComplexivo && this.esComplexivo()) return false;
        return true;
      })
      .map(sec => ({
        ...sec,
        items: sec.items.filter(it => {
          if (it.roles && !it.roles.some(r => userRoles.includes(r))) return false;
          if (it.soloNoComplexivo && this.esComplexivo()) return false;
          return true;
        }),
      }))
      .filter(sec => sec.items.length > 0);
  }

  private getNormalizedRoles(): AppRole[] {
    const validRoles: AppRole[] = [
      'ADMIN',
      'DOCENTE',
      'DOCENTE_TITULADO',
      'ESTUDIANTE',
      'COORDINADOR',
      'DECANO',
      'SECRETARIO_ABOGADO',
      'DIRECCION_ACADEMICA',
      'GESTOR_USUARIOS',
      'ROL_REPORT',
    ];

    return getUserRoles()
      .map(r => r.replace('ROLE_', '') as AppRole)
      .filter(r => validRoles.includes(r));
  }
}
