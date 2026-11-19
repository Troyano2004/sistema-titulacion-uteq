import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ComisionTemasService, TemaBancoDto } from '../../../services/comision-temas';
import { CatalogoCarrera, CatalogosService } from '../../../services/catalogos';
import { DocentesService } from '../../../services/docentes.service';
import { getSessionEntityId, getSessionUser, hasAnyRole } from '../../../services/session';

@Component({
  selector: 'app-banco-temas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './banco-temas.component.html',
  styleUrl: './banco-temas.component.scss'
})
export class BancoTemasComponent implements OnInit {

  // ── Identidad del usuario ──────────────────────────────────────────────
  // getUserRoles() devuelve los roles con prefijo ROLE_ en mayúsculas
  readonly esEstudiante = hasAnyRole('ROLE_ESTUDIANTE');
  readonly esComision   = hasAnyRole(
    'ROLE_DOCENTE',
    'ROLE_DOCENTE_TITULADO',
    'ROLE_COORDINADOR',
    'ROLE_ADMIN'
  );
  readonly esConsejoDirectivo = hasAnyRole('ROLE_DECANO', 'ROLE_COORDINADOR');

  idDocente    = getSessionEntityId(getSessionUser(), 'docente');
  idEstudiante = getSessionEntityId(getSessionUser(), 'estudiante');
  idUsuario    = Number(getSessionUser()?.['idUsuario']) || null;

  // ── Aprobación Consejo Directivo ───────────────────────────────────────
  aprobandoConsejoId: number | null = null;
  errorConsejo = '';

  // ── Banco de temas (comisión) ──────────────────────────────────────────
  temas           = signal<TemaBancoDto[]>([]);
  loading         = signal(false);
  saving          = signal(false);
  loadingCarreras = signal(false);
  carreras        = signal<CatalogoCarrera[]>([]);
  /** true si el docente autenticado no tiene ninguna carrera vinculada (docente_carrera vacío). */
  sinCarreraAsignada = false;
  error           = signal<string | null>(null);
  ok              = signal<string | null>(null);

  form = { idCarrera: 1, titulo: '', descripcion: '', observaciones: '' };

  // ── Sugerencias de estudiantes (comisión) ─────────────────────────────
  sugerencias          = signal<TemaBancoDto[]>([]);
  loadingSugerencias   = signal(false);
  sugerenciaEnRevision: TemaBancoDto | null = null;
  obsDecision          = '';
  procesandoDecision   = false;
  errorDecision        = '';

  // ── Formulario sugerencia (estudiante) ────────────────────────────────
  formSugerencia = { titulo: '', descripcion: '' };
  enviandoSug    = false;
  errorSug       = '';
  exitoSug       = '';

  constructor(
    private readonly api: ComisionTemasService,
    private readonly catalogosApi: CatalogosService,
    private readonly docentesApi: DocentesService
  ) {}

  ngOnInit(): void {
    if (this.esComision) {
      this.cargarCarreras();
      this.cargar();
      this.cargarSugerencias();
    }
  }

  // ── Banco oficial ──────────────────────────────────────────────────────

  /**
   * Un docente solo puede registrar temas para SU carrera (docente_carrera),
   * no para cualquiera de la universidad — por eso ya no se usa el catálogo
   * completo. Si no tiene ninguna carrera vinculada, se avisa en vez de
   * dejarlo elegir de una lista que no le corresponde. Coordinador/Admin no
   * tienen fila de docente, así que para ellos se mantiene el catálogo
   * completo (pueden registrar temas para cualquier carrera).
   */
  cargarCarreras(): void {
    this.loadingCarreras.set(true);
    const fuente$ = this.idDocente
      ? this.docentesApi.carrerasDe(this.idDocente)
      : this.catalogosApi.listarCarreras();

    fuente$.subscribe({
      next: (resp) => {
        const carreras = resp ?? [];
        this.carreras.set(carreras);
        this.sinCarreraAsignada = !!this.idDocente && carreras.length === 0;
        if (!carreras.some(c => c.idCarrera === this.form.idCarrera)) {
          this.form.idCarrera = carreras[0]?.idCarrera ?? this.form.idCarrera;
        }
        this.loadingCarreras.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar las carreras.');
        this.loadingCarreras.set(false);
      }
    });
  }

  cargar(): void {
    if (!this.idDocente) { this.error.set('No se pudo identificar al docente.'); return; }
    this.loading.set(true);
    this.error.set(null);
    this.api.listarBanco(this.idDocente).subscribe({
      next: (resp) => { this.temas.set(resp ?? []); this.loading.set(false); },
      error: (err)  => { this.error.set(err?.error?.message ?? 'Error al cargar temas.'); this.loading.set(false); }
    });
  }

  guardarTema(): void {
    if (!this.idDocente) { this.error.set('No se pudo identificar al docente.'); return; }
    if (!this.form.titulo.trim() || !this.form.descripcion.trim()) {
      this.error.set('Título y descripción son obligatorios.');
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.ok.set(null);
    this.api.crearTema(this.idDocente, {
      idCarrera:     this.form.idCarrera,
      titulo:        this.form.titulo,
      descripcion:   this.form.descripcion,
      observaciones: this.form.observaciones
    }).subscribe({
      next: () => {
        this.ok.set('Tema agregado correctamente al banco.');
        this.form.titulo = '';
        this.form.descripcion = '';
        this.form.observaciones = '';
        this.saving.set(false);
        this.cargar();
      },
      error: (err) => { this.error.set(err?.error?.message ?? 'Error al guardar.'); this.saving.set(false); }
    });
  }

  // ── Sugerencias (comisión) ─────────────────────────────────────────────

  cargarSugerencias(): void {
    if (!this.idDocente) return;
    this.loadingSugerencias.set(true);
    this.api.listarSugerencias(this.idDocente).subscribe({
      next: (resp) => { this.sugerencias.set(resp ?? []); this.loadingSugerencias.set(false); },
      error: ()    => { this.loadingSugerencias.set(false); }
    });
  }

  abrirDecision(s: TemaBancoDto): void {
    this.sugerenciaEnRevision = s;
    this.obsDecision   = '';
    this.errorDecision = '';
  }

  cerrarModal(): void {
    this.sugerenciaEnRevision = null;
    this.obsDecision   = '';
    this.errorDecision = '';
  }

  aprobar(): void {
    if (!this.sugerenciaEnRevision || !this.idDocente) return;
    this.procesandoDecision = true;
    this.api.aprobarSugerencia(this.idDocente, this.sugerenciaEnRevision.idTema, this.obsDecision).subscribe({
      next: () => {
        this.procesandoDecision = false;
        this.cerrarModal();
        this.cargar();
        this.cargarSugerencias();
      },
      error: (err) => {
        this.procesandoDecision = false;
        this.errorDecision = err?.error?.message ?? 'Error al aprobar.';
      }
    });
  }

  rechazar(): void {
    if (!this.sugerenciaEnRevision || !this.idDocente) return;
    this.procesandoDecision = true;
    this.api.rechazarSugerencia(this.idDocente, this.sugerenciaEnRevision.idTema, this.obsDecision).subscribe({
      next: () => {
        this.procesandoDecision = false;
        this.cerrarModal();
        this.cargarSugerencias();
      },
      error: (err) => {
        this.procesandoDecision = false;
        this.errorDecision = err?.error?.message ?? 'Error al rechazar.';
      }
    });
  }

  // ── Consejo Directivo de Facultad ──────────────────────────────────────

  puedeAprobarConsejo(tema: TemaBancoDto): boolean {
    return this.esConsejoDirectivo && (tema.estado === 'PROPUESTO' || tema.estado === 'APROBADO_COMISION');
  }

  aprobarConsejo(tema: TemaBancoDto): void {
    if (!this.idUsuario) { this.errorConsejo = 'No se pudo identificar tu sesión.'; return; }
    this.aprobandoConsejoId = tema.idTema;
    this.errorConsejo = '';
    this.api.aprobarPorConsejoDirectivo(tema.idTema, this.idUsuario).subscribe({
      next: () => {
        this.aprobandoConsejoId = null;
        this.cargar();
      },
      error: (err) => {
        this.aprobandoConsejoId = null;
        this.errorConsejo = err?.error?.message ?? 'No se pudo aprobar el tema por Consejo Directivo.';
      }
    });
  }

  // ── Sugerencia (estudiante) ────────────────────────────────────────────

  enviarSugerencia(): void {
    this.errorSug = '';
    this.exitoSug = '';

    if (!this.formSugerencia.titulo.trim())     { this.errorSug = 'El título es obligatorio.'; return; }
    if (!this.formSugerencia.descripcion.trim()) { this.errorSug = 'La descripción es obligatoria.'; return; }
    if (!this.idEstudiante)                      { this.errorSug = 'No se pudo identificar tu sesión.'; return; }

    this.enviandoSug = true;
    this.api.sugerirTema(this.idEstudiante, this.formSugerencia.titulo, this.formSugerencia.descripcion).subscribe({
      next: () => {
        this.enviandoSug = false;
        this.exitoSug = '¡Sugerencia enviada! La comisión la revisará pronto.';
        this.formSugerencia = { titulo: '', descripcion: '' };
      },
      error: (err) => {
        this.enviandoSug = false;
        this.errorSug = err?.error?.message ?? 'Error al enviar la sugerencia.';
      }
    });
  }
}
