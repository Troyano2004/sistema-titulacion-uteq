import { Routes } from '@angular/router';
import { authGuard, loginGuard, roleGuard } from './guards/auth.guard';
import { isAuthenticated } from './services/session';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then(m => m.LoginComponent),
    canActivate: [loginGuard],
    data: { title: 'Login' },
  },
  {
    path: 'recovery',
    loadComponent: () => import('./pages/backup/recovery/recovery.component').then(m => m.RecoveryComponent),
    data: { title: 'Recovery Mode' },
  },
  {
    path: 'registro-estudiante',
    loadComponent: () => import('./pages/registro-estudiante/registro-estudiante').then(m => m.RegistroEstudianteComponent),
    data: { title: 'Registro Estudiante' },
  },
  {
    path: 'app',
    loadComponent: () => import('./layout/shell/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () => import('./pages/menu-home/menu-home.component').then(m => m.MenuHomeComponent),
        data: { title: 'Inicio' },
      },
      {
        path: 'menu/:slug',
        loadComponent: () => import('./pages/menu-section/menu-section.component').then(m => m.MenuSectionComponent),
        data: { title: 'Menú' },
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
        data: { title: 'Dashboard' },
      },

      // ── Estudiante / Anteproyecto ──────────────────────────────────────────
      {
        path: 'anteproyecto/nuevo',
        loadComponent: () => import('./pages/anteproyecto/anteproyecto').then(m => m.AnteproyectoComponent),
        canActivate: [roleGuard('ADMIN', 'ESTUDIANTE')],
        data: { title: 'Registrar anteproyecto' },
      },
      {
        path: 'tutorias/historial',
        loadComponent: () => import('./pages/historialtutorias/historialtutorias').then(m => m.Historialtutorias),
        canActivate: [roleGuard('ADMIN', 'ESTUDIANTE')],
        data: { title: 'Historial de Tutorías' },
      },
      {
        path: 'titulacion/matricula',
        loadComponent: () => import('./pages/titulacion/matricula/matricula').then(m => m.MatriculaTitulacionComponent),
        canActivate: [roleGuard('ADMIN', 'ESTUDIANTE')],
        data: { title: 'Matrícula de Titulación' },
      },
      {
        path: 'titulacion/cambio-opcion',
        loadComponent: () => import('./pages/titulacion/cambio-opcion/cambio-opcion').then(m => m.CambioOpcionTitulacionComponent),
        canActivate: [roleGuard('ADMIN', 'ESTUDIANTE')],
        data: { title: 'Cambio de Opción de Titulación' },
      },

      // ── Director / DT1 ────────────────────────────────────────────────────
      {
        path: 'director/mis-anteproyectos',
        loadComponent: () => import('./pages/director/directoranteproyectos/directoranteproyectos').then(m => m.DirectorMisAnteproyectosComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Mis anteproyectos' },
      },
      {
        path: 'director/tutorias',
        loadComponent: () => import('./pages/director/tutoriasdirector/tutoriasdirector').then(m => m.Tutoriasdirector),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Tutorías (Director)' },
      },
      {
        path: 'director/acta',
        loadComponent: () => import('./pages/director/actadirector/actadirector').then(m => m.Actadirector),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Acta de revisión (Director)' },
      },
      {
        path: 'dt1/lista',
        loadComponent: () => import('./pages/dt1/dt1lista/dt1lista').then(m => m.Dt1EnviadosComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'DT1 - Lista' },
      },
      {
        path: 'dt1/revision',
        loadComponent: () => import('./pages/dt1/dt1revision/dt1revision').then(m => m.Dt1RevisionComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'DT1 - Revisión' },
      },

      // ── Titulación II — DT2 ───────────────────────────────────────────────
      {
        path: 'coordinador/configuracion-dt2',
        loadComponent: () => import('./pages/coordinador/configuracion-dt2/configuracion-dt2').then(m => m.ConfiguracionDt2Component),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'DT2 - Configuración Inicial' },
      },
      {
        path: 'director/seguimiento-dt2',
        loadComponent: () => import('./pages/director/seguimiento-dt2/seguimiento-dt2').then(m => m.SeguimientoDt2Component),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'DT2 - Seguimiento de Avances' },
      },
      {
        path: 'director/antiplagio-dt2',
        loadComponent: () => import('./pages/director/antiplagio-dt2/antiplagio-dt2').then(m => m.AntiplagioDt2Component),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'DT2 - Antiplagio COMPILATIO' },
      },
      {
        path: 'titulacion2/predefensa',
        loadComponent: () => import('./pages/titulacion2/predefensa/predefensa-dt2').then(m => m.PredefensaDt2Component),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'COORDINADOR')],
        data: { title: 'DT2 - Predefensa' },
      },
      {
        path: 'titulacion2/sustentacion',
        loadComponent: () => import('./pages/titulacion2/sustentacion/sustentacion-dt2').then(m => m.SustentacionDt2Component),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'COORDINADOR')],
        data: { title: 'DT2 - Sustentación Final' },
      },

      // ── Titulación II — Documento / Revisión ──────────────────────────────
      {
        path: 'titulacion2/documento',
        loadComponent: () => import('./pages/titulacion2/documento/documento').then(m => m.Documento),
        canActivate: [roleGuard('ADMIN', 'ESTUDIANTE')],
        data: { title: 'Documento de titulación' },
      },
      {
        path: 'titulacion2/documento-tic',
        loadComponent: () => import('./pages/titulacion2/editor-tic/editor-tic').then(m => m.EditorTicComponent),
        canActivate: [roleGuard('ADMIN', 'ESTUDIANTE', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Editor del Documento del TIC' },
      },
      {
        path: 'titulacion2/documento-tic/:id',
        loadComponent: () => import('./pages/titulacion2/editor-tic/editor-tic').then(m => m.EditorTicComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Revisión del Documento del TIC' },
      },
      {
        path: 'titulacion2/bibliografia',
        loadComponent: () => import('./pages/titulacion2/bibliografia-tic/bibliografia-tic').then(m => m.BibliografiaTicComponent),
        canActivate: [roleGuard('ADMIN', 'ESTUDIANTE')],
        data: { title: 'Bibliografía del TIC' },
      },
      {
        path: 'titulacion2/revisar',
        loadComponent: () => import('./pages/titulacion2/revision/revision').then(m => m.Revision),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Revisión de director' },
      },
      {
        path: 'titulacion2/revisar/:idDocumento',
        loadComponent: () => import('./pages/titulacion2/revision-detalle/revision-detalle').then(m => m.RevisionDetalle),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Detalle de revisión' },
      },
      { path: 'titulacion2/revision',              pathMatch: 'full', redirectTo: 'titulacion2/revisar' },
      { path: 'titulacion2/revision/:idDocumento', pathMatch: 'full', redirectTo: 'titulacion2/revisar/:idDocumento' },

      // ── Complexivo ────────────────────────────────────────────────────────
      {
        path: 'complexivo/informe',
        loadComponent: () => import('./pages/complexivo/informe/complexivo-informe').then(m => m.ComplexivoInformeComponent),
        canActivate: [roleGuard('ADMIN', 'ESTUDIANTE')],
        data: { title: 'Complexivo - Informe Práctico' },
      },
      {
        path: 'complexivo/mis-estudiantes',
        loadComponent: () => import('./pages/complexivo/docente/complexivo-docente').then(m => m.ComplexivoDocenteComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Complexivo - Mis Estudiantes' },
      },

      // ── Estudiantes ───────────────────────────────────────────────────────
      {
        path: 'estudiantes',
        loadComponent: () => import('./pages/estudiantes/estudiantes').then(m => m.EstudiantesComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Estudiantes' },
      },

      // ── Catálogos Académicos ───────────────────────────────────────────────
      {
        path: 'catalogos/universidad',
        loadComponent: () => import('./pages/catalogos/universidad/universidad.component').then(m => m.UniversidadComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Universidad' },
      },
      {
        path: 'catalogos/facultad',
        loadComponent: () => import('./pages/catalogos/facultad/facultad.component').then(m => m.FacultadComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Facultad' },
      },
      {
        path: 'catalogos/docentes',
        loadComponent: () => import('./pages/catalogos/docentes/docentes.component').then(m => m.DocentesCarrerasComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Docentes y Carreras' },
      },
      {
        path: 'catalogos/carrera',
        loadComponent: () => import('./pages/catalogos/carrera/carrera.component').then(m => m.CarreraComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Carrera' },
      },
      {
        path: 'catalogos/modalidad',
        loadComponent: () => import('./pages/catalogos/modalidad/modalidad-catalogo.component').then(m => m.ModalidadCatalogoComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Modalidad Titulación' },
      },
      {
        path: 'catalogos/periodo',
        loadComponent: () => import('./pages/catalogos/periodo/periodo.component').then(m => m.PeriodoComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Período Académico' },
      },
      {
        path: 'catalogos/tipo-trabajo',
        loadComponent: () => import('./pages/catalogos/tipo-trabajo/tipo-trabajo.component').then(m => m.TipoTrabajoComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Tipo Trabajo Titulación' },
      },
      {
        path: 'catalogos/carrera-modalidad',
        loadComponent: () => import('./pages/catalogos/carrera-modalidad/carrera-modalidad.component').then(m => m.CarreraModalidadComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Carrera-Modalidad' },
      },

      // ── Banco de Temas ─────────────────────────────────────────────────────
      {
        path: 'temas',
        loadComponent: () => import('./pages/temas/banco-temas/banco-temas.component').then(m => m.BancoTemasComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Banco de temas' },
      },
      {
        path: 'temas/nuevo',
        loadComponent: () => import('./pages/temas/banco-temas/banco-temas.component').then(m => m.BancoTemasComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Registrar tema' },
      },
      {
        path: 'temas/aprobacion',
        loadComponent: () => import('./pages/temas/aprobacion-temas/revision-propuestas.component').then(m => m.RevisionPropuestasComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Aprobación de propuestas' },
      },
      {
        path: 'temas/mis-propuestas',
        loadComponent: () => import('./pages/temas/sugerencia-tema/sugerencia-tema.component').then(m => m.SugerenciaTemaComponent),
        canActivate: [roleGuard('ADMIN', 'ESTUDIANTE')],
        data: { title: 'Mis propuestas de tema' },
      },

      // ── Propuesta y Anteproyecto ───────────────────────────────────────────
      {
        path: 'propuesta/pendientes',
        loadComponent: () => import('./pages/propuestas-pendientes/propuestas-pendientes.component').then(m => m.PropuestasPendientesComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Propuestas pendientes' },
      },
      {
        path: 'propuesta/nueva',
        loadComponent: () => import('./pages/propuesta/propuesta-nueva/propuesta-nueva.component').then(m => m.PropuestaNuevaComponent),
        canActivate: [roleGuard('ADMIN', 'ESTUDIANTE')],
        data: { title: 'Registrar propuesta' },
      },
      {
        path: 'propuesta/revision',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Revisión por director' },
      },
      {
        path: 'propuesta/historial',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Historial observaciones' },
      },
      {
        path: 'propuesta/reporte',
        loadComponent: () => import('./pages/propuesta/reporte-propuestas/reporte-propuestas.component').then(m => m.ReportePropuestasComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Reporte de propuestas' },
      },

      // ── Tutorías ──────────────────────────────────────────────────────────
      {
        path: 'tutorias/nueva',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Registrar tutoría' },
      },
      {
        path: 'tutorias/actas',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Actas de tutoría' },
      },

      // ── Proyecto de Titulación ─────────────────────────────────────────────
      {
        path: 'proyecto/documento',
        loadComponent: () => import('./pages/documento-secciones/documento-secciones.component').then(m => m.DocumentoSeccionesComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Documento por secciones' },
      },
      {
        path: 'proyecto/revision',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Revisión por secciones' },
      },
      {
        path: 'proyecto/correcciones',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Correcciones' },
      },
      {
        path: 'proyecto/estado',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Estado del proyecto' },
      },

      // ── Documentos ────────────────────────────────────────────────────────
      {
        path: 'documentos/habilitantes',
        loadComponent: () => import('./pages/habilitantes/habilitantes').then(m => m.DocumentosHabilitantesComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'ESTUDIANTE', 'SECRETARIO_ABOGADO')],
        data: { title: 'Habilitantes' },
      },
      {
        path: 'documentos/versiones',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'ESTUDIANTE', 'SECRETARIO_ABOGADO')],
        data: { title: 'Versiones' },
      },
      {
        path: 'documentos/expediente',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'ESTUDIANTE', 'SECRETARIO_ABOGADO')],
        data: { title: 'Expediente' },
      },

      // ── Legalización ──────────────────────────────────────────────────────
      {
        path: 'legalizacion/validacion',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DECANO', 'SECRETARIO_ABOGADO')],
        data: { title: 'Validación legal' },
      },
      {
        path: 'legalizacion/checklist',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DECANO', 'SECRETARIO_ABOGADO')],
        data: { title: 'Checklist' },
      },
      {
        path: 'legalizacion/aprobacion',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DECANO', 'SECRETARIO_ABOGADO')],
        data: { title: 'Aprobación final' },
      },

      // ── Reportes ──────────────────────────────────────────────────────────
      {
        path: 'reportes/expediente',
        loadComponent: () => import('./pages/reportes/expediente/expediente.component').then(m => m.ExpedienteComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'COORDINADOR', 'DECANO', 'DIRECCION_ACADEMICA', 'ROL_REPORT')],
        data: { title: 'Expediente por estudiante' },
      },
      {
        path: 'reportes/periodo',
        loadComponent: () => import('./pages/reportes/periodo/periodo.component').then(m => m.ReportePeriodoComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'COORDINADOR', 'DECANO', 'DIRECCION_ACADEMICA', 'ROL_REPORT')],
        data: { title: 'Por período' },
      },
      {
        path: 'reportes/actas',
        loadComponent: () => import('./pages/reportes/actas/actas.component').then(m => m.ActasComponent),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO', 'COORDINADOR', 'DECANO', 'DIRECCION_ACADEMICA', 'ROL_REPORT')],
        data: { title: 'Actas y constancias' },
      },
      {
        path: 'reportes/dashboard-institucional',
        loadComponent: () => import('./pages/reportes/dashboard-institucional/dashboard-institucional.component').then(m => m.DashboardInstitucionalComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DIRECCION_ACADEMICA')],
        data: { title: 'Dashboard institucional' },
      },
      {
        path: 'reportes/trazabilidad',
        loadComponent: () => import('./pages/reportes/trazabilidad/trazabilidad.component').then(m => m.TrazabilidadComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Trazabilidad de estudiante' },
      },

      // ── Administración ────────────────────────────────────────────────────
      {
        path: 'admin/usuarios',
        loadComponent: () => import('./pages/admin-usuario/admin-usuarios').then(m => m.AdminUsuariosComponent),
        canActivate: [roleGuard('ADMIN', 'GESTOR_USUARIOS')],
        data: { title: 'Usuarios' },
      },
      {
        path: 'admin/roles',
        loadComponent: () => import('./pages/roles/roles').then(m => m.RolesComponent),
        canActivate: [roleGuard('ADMIN', 'GESTOR_USUARIOS')],
        data: { title: 'Roles del aplicativo' },
      },
      {
        path: 'admin/parametros',
        loadComponent: () => import('./pages/parametros/parametros.component').then(m => m.ParametrosComponent),
        canActivate: [roleGuard('ADMIN', 'GESTOR_USUARIOS')],
        data: { title: 'Parámetros' },
      },
      {
        path: 'admin/gestion-solicitudes',
        loadComponent: () => import('./pages/gestion-solicitudes/gestion-solicitudes').then(m => m.GestionSolicitudesComponent),
        canActivate: [roleGuard('ADMIN', 'GESTOR_USUARIOS')],
        data: { title: 'Gestión de Solicitudes' },
      },
      {
        path: 'admin/configuracion-correo',
        loadComponent: () => import('./pages/configuracion-correo/configuracion-correo').then(m => m.ConfiguracionCorreoComponent),
        canActivate: [roleGuard('ADMIN')],
        data: { title: 'Configuración de Correo' },
      },
      {
        path: 'admin/backup',
        loadComponent: () => import('./pages/backup/backup-jobs/backup-jobs.component').then(m => m.BackupJobsComponent),
        canActivate: [roleGuard('ADMIN')],
        data: { title: 'Respaldos de Base de Datos' },
      },
      {
        path: 'admin/backup/restaurar',
        loadComponent: () => import('./pages/backup/backup-restore/backup-restore.component').then(m => m.BackupRestoreComponent),
        canActivate: [roleGuard('ADMIN')],
        data: { title: 'Restauración de Backups' },
      },
      // Auditoría: el backend (AuditController) exige ADMIN estrictamente —
      // se restringe igual aquí para no mostrar una página que el API rechazará.
      {
        path: 'admin/auditoria/dashboard',
        loadComponent: () => import('./pages/auditoria/audit-dashboard/audit-dashboard').then(m => m.AuditDashboardComponent),
        canActivate: [roleGuard('ADMIN')],
        data: { title: 'Dashboard Auditoría' },
      },
      {
        path: 'admin/auditoria/logs',
        loadComponent: () => import('./pages/auditoria/audit-logs/audit-logs').then(m => m.AuditLogsComponent),
        canActivate: [roleGuard('ADMIN')],
        data: { title: 'Logs de Auditoría' },
      },
      {
        path: 'admin/auditoria/config',
        loadComponent: () => import('./pages/auditoria/audit-config/audit-config').then(m => m.AuditConfigComponent),
        canActivate: [roleGuard('ADMIN')],
        data: { title: 'Configuración Auditoría' },
      },

      // ── Coordinación ──────────────────────────────────────────────────────
      {
        path: 'coordinador/seguimiento',
        loadComponent: () => import('./pages/coordinador/seguimiento-proyectos/seguimiento-proyectos.component').then(m => m.SeguimientoProyectosComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Seguimiento de proyectos' },
      },
      {
        path: 'coordinador/directores',
        loadComponent: () => import('./pages/coordinador/directores/directores.component').then(m => m.DirectoresComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Control de directores' },
      },
      {
        path: 'coordinador/validacion',
        loadComponent: () => import('./pages/coordinador/validacion/validacion.component').then(m => m.ValidacionComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Validación administrativa' },
      },
      {
        path: 'coordinador/tutorias',
        loadComponent: () => import('./pages/coordinador/tutorias-control/tutorias-control.component').then(m => m.TutoriasControlComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Control de tutorías' },
      },
      {
        path: 'coordinador/listado-selecciones',
        loadComponent: () => import('./pages/coordinador/listado-selecciones/listado-selecciones').then(m => m.ListadoSeleccionesComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DECANO', 'DIRECCION_ACADEMICA')],
        data: { title: 'Listado de selecciones' },
      },
      {
        path: 'coordinador/designacion-consejo-directivo',
        loadComponent: () => import('./pages/coordinador/designacion-consejo-directivo/designacion-consejo-directivo').then(m => m.DesignacionConsejoDirectivoComponent),
        canActivate: [roleGuard('ADMIN', 'DECANO', 'COORDINADOR')],
        data: { title: 'Designación Consejo Directivo' },
      },
      {
        path: 'coordinador/observaciones',
        loadComponent: () => import('./pages/coordinador/observaciones-admin/observaciones-admin.component').then(m => m.ObservacionesAdminComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Observaciones administrativas' },
      },
      {
        path: 'coordinador/reportes',
        loadComponent: () => import('./pages/coordinador/reportes/reportes.component').then(m => m.ReportesCoordinacionComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Reportes de coordinación' },
      },
      {
        path: 'coordinador/comision',
        loadComponent: () => import('./pages/coordinador/comision-formativa/comision-formativa.component').then(m => m.ComisionFormativaComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Comisión formativa' },
      },
      {
        path: 'coordinador/proyecto',
        loadComponent: () => import('./pages/coordinador/visualizar-proyecto/visualizar-proyecto.component').then(m => m.VisualizarProyectoComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Visualización de proyecto' },
      },
      {
        path: 'coordinador/dt1-asignacion',
        loadComponent: () => import('./pages/coordinador/asignacion-dt1/asignacion-dt1').then(m => m.AsignacionDt1),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'DT1 - Asignación Docentes y Tutores' },
      },
      {
        path: 'coordinador/complexivo-asignacion',
        loadComponent: () => import('./pages/coordinador/asignacion-complexivo/asignacion-complexivo').then(m => m.AsignacionComplexivoComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Complexivo - Asignación Docente' },
      },
      {
        path: 'coordinador/complexivo-dt2-asignacion',
        loadComponent: () => import('./pages/coordinador/asignacion-complexivo-dt2/asignacion-complexivo-dt2').then(m => m.AsignacionComplexivoDt2Component),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Complexivo - Asignación DT2' },
      },
      {
        path: 'titulacion2/complexivo-informes',
        loadComponent: () => import('./pages/complexivo/dt2/complexivo-dt2').then(m => m.ComplexivoDt2Component),
        canActivate: [roleGuard('ADMIN', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Complexivo - Informes Prácticos' },
      },

      // ── Examen Complexivo real (100 preguntas) ─────────────────────────────
      {
        path: 'complexivo-real/banco-preguntas',
        loadComponent: () => import('./pages/docente/banco-preguntas/banco-preguntas').then(m => m.BancoPreguntasComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Banco de Preguntas - Complexivo' },
      },
      {
        path: 'complexivo-real/rendir',
        loadComponent: () => import('./pages/estudiante/examen-complexivo/examen-complexivo').then(m => m.ExamenComplexivoEstudianteComponent),
        canActivate: [roleGuard('ADMIN', 'ESTUDIANTE')],
        data: { title: 'Examen Complexivo' },
      },
      {
        path: 'director-admin/examen-complexivo',
        loadComponent: () => import('./pages/direccion-academica/gestion-complexivo/gestion-complexivo').then(m => m.GestionComplexivoComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DECANO', 'DIRECCION_ACADEMICA')],
        data: { title: 'Gestión del Examen Complexivo' },
      },

      // ── Calendario de titulación (Módulo 18) ────────────────────────────────
      {
        path: 'calendario-titulacion',
        loadComponent: () => import('./pages/calendario/calendario-titulacion/calendario-titulacion').then(m => m.CalendarioTitulacionComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DECANO', 'DIRECCION_ACADEMICA', 'DOCENTE', 'DOCENTE_TITULADO')],
        data: { title: 'Calendario de Titulación' },
      },

      // ── Expediente digital (Módulo 15/16) ───────────────────────────────────
      {
        path: 'expediente/mi-expediente',
        loadComponent: () => import('./pages/expediente/expediente-estudiante/expediente-estudiante').then(m => m.ExpedienteEstudianteComponent),
        canActivate: [roleGuard('ADMIN', 'ESTUDIANTE')],
        data: { title: 'Mi Expediente Digital' },
      },
      {
        path: 'expediente/validacion',
        loadComponent: () => import('./pages/expediente/validacion-expediente/validacion-expediente').then(m => m.ValidacionExpedienteComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'SECRETARIO_ABOGADO', 'DECANO')],
        data: { title: 'Validación de Expedientes' },
      },

      // ── Incorporación y registro (Módulo 19) ────────────────────────────────
      {
        path: 'incorporacion/panel',
        loadComponent: () => import('./pages/incorporacion/panel-incorporacion/panel-incorporacion').then(m => m.PanelIncorporacionComponent),
        canActivate: [roleGuard('ADMIN', 'SECRETARIO_ABOGADO', 'DIRECCION_ACADEMICA', 'DECANO')],
        data: { title: 'Incorporación y Registro de Títulos' },
      },

      // ── Integración con el SGA (Módulo 20) ──────────────────────────────────
      {
        path: 'sga/importacion',
        loadComponent: () => import('./pages/sga/importacion-sga/importacion-sga').then(m => m.ImportacionSgaComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR')],
        data: { title: 'Importación de datos del SGA' },
      },

      // ── Rutas adicionales ─────────────────────────────────────────────────
      {
        path: 'legal/validacion',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'COORDINADOR', 'DECANO', 'SECRETARIO_ABOGADO')],
        data: { title: 'Validación jurídica' },
      },
      {
        path: 'secretaria/actas',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'SECRETARIO_ABOGADO')],
        data: { title: 'Registro de actas' },
      },
      {
        path: 'secretaria/documentos',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'SECRETARIO_ABOGADO')],
        data: { title: 'Gestión documental' },
      },
      {
        path: 'director-admin/gestion',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'DECANO', 'DIRECCION_ACADEMICA')],
        data: { title: 'Gestión institucional' },
      },
      {
        path: 'director-admin/reportes',
        loadComponent: () => import('./pages/dashboard/placeholder/placeholder.component').then(m => m.PlaceholderPageComponent),
        canActivate: [roleGuard('ADMIN', 'DECANO', 'DIRECCION_ACADEMICA')],
        data: { title: 'Aprobación de reportes' },
      },

      { path: '**', redirectTo: 'inicio' }
    ]
  },
  // Cualquier URL rota o inexistente: si ya hay sesión, manda al menú
  // principal en vez de forzar un re-login (un usuario logueado que cae
  // aquí por un enlace roto no debería perder la sesión).
  { path: '**', redirectTo: () => (isAuthenticated() ? '/app/inicio' : '/login') }
];
