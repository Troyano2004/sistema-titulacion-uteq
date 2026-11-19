
import { Component, OnInit, signal, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import {
  ComisionTemasService,
  EstadoModalidadDto,
  FeedbackIAPropuesta,
  PropuestaTemaDto,
  ReferenciaBibliograficaRequest,
  RevisionPropuestaIAResponse,
  TemaBancoDto
} from '../../../services/comision-temas';
import { CatalogosBasicosService, PeriodoTitulacion } from '../../../services/catalogos-basicos.service';
import { ConfiguracionTitulacionService, ModalidadEntrega } from '../../../services/configuracion-titulacion.service';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

export interface ReferenciaBibliografica {
  texto: string;
  anio: number | null;
}

@Component({
  selector: 'app-propuesta-nueva',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './propuesta-nueva.component.html',
  styleUrl: './propuesta-nueva.component.scss'
})
export class PropuestaNuevaComponent implements OnInit {

  idEstudiante = getSessionEntityId(getSessionUser(), 'estudiante');

  historial           = signal<PropuestaTemaDto[]>([]);
  temasDisponibles    = signal<TemaBancoDto[]>([]);
  loading             = signal(false);
  loadingTemas        = signal(false);
  loadingModalidad    = signal(false);
  saving              = signal(false);
  guardandoModalidad  = signal(false);
  error               = signal<string | null>(null);
  ok                  = signal<string | null>(null);
  estadoModalidad     = signal<EstadoModalidadDto | null>(null);
  modalidadSeleccionada = signal<number | null>(null);

  // ── Estado del panel IA (revisión PREVIA, antes de enviar) ──────────────
  analizandoIA        = signal(false);
  respuestaIA         = signal<RevisionPropuestaIAResponse | null>(null);
  feedbackIA          = signal<FeedbackIAPropuesta | null>(null);
  errorIA             = signal<string | null>(null);
  modoIA: 'integral' | 'coherencia' | 'pertinencia' | 'viabilidad' = 'integral';
  instruccionIA = '';

  // ── Formulario ────────────────────────────────────────────────────────
  form = {
    idCarrera: 1,
    idTema: null as number | null,
    titulo: '',
    temaInvestigacion: '',
    planteamientoProblema: '',
    objetivosGenerales: '',
    objetivosEspecificos: '',
    metodologia: '',
    resultadosEsperados: '',
    bibliografia: ''
  };

  // ── Módulo 6: modalidad de entrega y plazo semanas 01-04 ─────────────────
  modalidadEntrega    = signal<ModalidadEntrega>('REDACCION_SISTEMA');
  periodoActivo       = signal<PeriodoTitulacion | null>(null);

  archivoPdf: File | null = null;
  subiendoPdf          = signal(false);
  urlPdfSubido          = signal<string | null>(null);
  errorPdf              = signal<string | null>(null);

  // ── Referencias bibliográficas (REDACCION_SISTEMA) ───────────────────────
  referencias: ReferenciaBibliografica[] = [{ texto: '', anio: null }];

  constructor(
    private readonly api: ComisionTemasService,
    private readonly catalogosBasicosApi: CatalogosBasicosService,
    private readonly configuracionTitulacionApi: ConfiguracionTitulacionService,
    private readonly http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarEstadoModalidad();
    this.cargarTemasDisponibles();
    this.cargarHistorial();
    this.cargarPeriodoActivo();
  }

  // ── Plazo semana 01-04 ────────────────────────────────────────────────

  cargarPeriodoActivo(): void {
    this.catalogosBasicosApi.listarPeriodosActivos().subscribe({
      next: periodos => {
        this.periodoActivo.set(periodos?.[0] ?? null);
        this.cargarModalidadEntrega();
        this.cdr.detectChanges();
      }
    });
  }

  cargarModalidadEntrega(): void {
    const idPeriodo = this.periodoActivo()?.idPeriodo;
    if (!idPeriodo || !this.form.idCarrera) return;
    this.configuracionTitulacionApi.obtenerModalidadEntrega(this.form.idCarrera, idPeriodo).subscribe({
      next: cfg => { this.modalidadEntrega.set(cfg.modalidadEntrega); this.cdr.detectChanges(); },
      error: () => { /* se mantiene el default REDACCION_SISTEMA */ }
    });
  }

  get semanaActual(): number {
    const fechaInicio = this.periodoActivo()?.fechaInicio;
    if (!fechaInicio) return 0;
    const inicio = new Date(fechaInicio);
    const hoy = new Date();
    const dias = Math.floor((hoy.getTime() - inicio.getTime()) / 86400000);
    if (dias < 0) return 0;
    return Math.floor(dias / 7) + 1;
  }

  get dentroDePlazoPropuesta(): boolean {
    const s = this.semanaActual;
    return s >= 1 && s <= 4;
  }

  // ── Referencias bibliográficas ───────────────────────────────────────

  agregarReferencia(): void { this.referencias.push({ texto: '', anio: null }); }

  quitarReferencia(i: number): void { this.referencias.splice(i, 1); }

  get totalReferencias(): number {
    return this.referencias.filter(r => r.texto.trim() && r.anio).length;
  }

  get referenciasRecientes(): number {
    const limite = new Date().getFullYear() - 5;
    return this.referencias.filter(r => r.texto.trim() && r.anio && r.anio >= limite).length;
  }

  get porcentajeRecientes(): number {
    return this.totalReferencias === 0 ? 0 : Math.round((this.referenciasRecientes / this.totalReferencias) * 100);
  }

  get bibliografiaValida(): boolean {
    return this.totalReferencias >= 10 && this.porcentajeRecientes >= 60;
  }

  private serializarReferencias(): string {
    return this.referencias
      .filter(r => r.texto.trim())
      .map((r, i) => `${i + 1}. ${r.texto.trim()}${r.anio ? ' (' + r.anio + ')' : ''}`)
      .join('\n');
  }

  /**
   * El backend (Art. 46) valida la lista estructurada, no el texto libre.
   * El formulario solo pide "referencia" + "año" en un único campo; se manda
   * ese mismo texto como autores y título (el campo no distingue ambos) para
   * no rediseñar el formulario que ya tienen los usuarios.
   */
  private referenciasParaBackend(): ReferenciaBibliograficaRequest[] {
    return this.referencias
      .filter(r => r.texto.trim() && r.anio)
      .map(r => ({
        autores: r.texto.trim(),
        titulo: r.texto.trim(),
        anioPublicacion: r.anio as number,
        tipo: 'OTRO'
      }));
  }

  // ── Modalidad UPLOAD_PDF ─────────────────────────────────────────────

  onArchivoSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.archivoPdf = input.files?.[0] ?? null;
    this.urlPdfSubido.set(null);
    this.errorPdf.set(null);
  }

  subirPdf(): void {
    if (!this.archivoPdf) { this.errorPdf.set('Selecciona un archivo PDF.'); return; }
    const formData = new FormData();
    formData.append('file', this.archivoPdf);

    this.subiendoPdf.set(true);
    this.errorPdf.set(null);
    this.http.post<{ url: string; filename: string }>('http://localhost:8080/api/uploads/files', formData)
      .pipe(finalize(() => { this.subiendoPdf.set(false); this.cdr.detectChanges(); }))
      .subscribe({
        next: resp => this.urlPdfSubido.set(resp.url),
        error: err => this.errorPdf.set(err?.error?.message ?? 'No se pudo subir el archivo.')
      });
  }

  // ── Getters ──────────────────────────────────────────────────────────

  get nombreCarreraSeleccionada(): string {
    return this.estadoModalidad()?.nombreCarrera ?? '';
  }

  get temasFiltradosPorCarrera(): TemaBancoDto[] {
    const nombre = this.normalizarTexto(this.nombreCarreraSeleccionada);
    if (!nombre) return this.temasDisponibles();
    return this.temasDisponibles().filter(t => this.normalizarTexto(t.carrera) === nombre);
  }

  get tieneModalidadSeleccionada(): boolean {
    return this.estadoModalidad()?.tieneModalidad ?? false;
  }

  get colorEstado(): Record<string, boolean> {
    const e = this.feedbackIA()?.estado_evaluacion;
    return {
      'estado-aprobable':        e === 'APROBABLE',
      'estado-requiere-ajustes': e === 'REQUIERE_AJUSTES',
      'estado-rechazable':       e === 'RECHAZABLE'
    };
  }

  get etiquetaModo(): string {
    const modos: Record<string, string> = {
      integral:    'Evaluación integral',
      coherencia:  'Coherencia interna',
      pertinencia: 'Pertinencia a la carrera',
      viabilidad:  'Viabilidad del proyecto'
    };
    return modos[this.modoIA] ?? 'Integral';
  }

  /** Muestra el panel IA si ya hay un análisis previo o está analizando */
  get mostrarPanelIA(): boolean {
    return this.analizandoIA() || this.feedbackIA() !== null || this.errorIA() !== null;
  }

  // ── Cargar datos ─────────────────────────────────────────────────────

  /**
   * La carrera de la propuesta es SIEMPRE la del estudiante autenticado — ya
   * no se deja elegir de un catálogo completo (el backend de todos modos
   * ignora cualquier idCarrera que no sea el del estudiante al crear la
   * propuesta; dejar un <select> ahí solo confundía).
   */
  cargarEstadoModalidad(): void {
    if (!this.idEstudiante) { this.error.set('No se pudo identificar al estudiante.'); return; }
    this.loadingModalidad.set(true);
    this.api.obtenerEstadoModalidad(this.idEstudiante).subscribe({
      next: estado => {
        this.estadoModalidad.set(estado);
        this.modalidadSeleccionada.set(estado.idModalidad);
        if (estado.idCarrera) this.form.idCarrera = estado.idCarrera;
        this.loadingModalidad.set(false);
        this.cargarModalidadEntrega();
        this.cdr.detectChanges();
      },
      error: () => { this.loadingModalidad.set(false); this.cdr.detectChanges(); }
    });
  }

  cargarTemasDisponibles(): void {
    if (!this.idEstudiante) return;
    this.loadingTemas.set(true);
    this.api.listarTemasDisponiblesEstudiante(this.idEstudiante).subscribe({
      next: data => {
        this.temasDisponibles.set(data ?? []);
        this.loadingTemas.set(false);
        this.cdr.detectChanges();
      },
      error: () => { this.loadingTemas.set(false); this.cdr.detectChanges(); }
    });
  }

  onSeleccionTema(): void {
    const tema = this.temasDisponibles().find(t => t.idTema === this.form.idTema);
    if (!tema) return;
    if (!this.form.titulo.trim())            this.form.titulo = tema.titulo;
    if (!this.form.temaInvestigacion.trim()) this.form.temaInvestigacion = tema.titulo;
  }

  cargarHistorial(): void {
    if (!this.idEstudiante) return;
    this.loading.set(true);
    this.api.listarPropuestasEstudiante(this.idEstudiante).subscribe({
      next: resp => {
        this.historial.set(resp ?? []);
        this.loading.set(false);
        this.cdr.detectChanges();
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'No se pudo cargar historial.');
        this.loading.set(false);
        this.cdr.detectChanges();
      }
    });
  }

  // ── Modalidad ─────────────────────────────────────────────────────────

  guardarModalidad(): void {
    const idModalidad = this.modalidadSeleccionada();
    if (!this.idEstudiante) { this.error.set('No se pudo identificar al estudiante.'); return; }
    if (!idModalidad) { this.error.set('Selecciona una modalidad.'); return; }

    this.guardandoModalidad.set(true);
    this.api.seleccionarModalidad(this.idEstudiante, idModalidad).subscribe({
      next: (estado) => {
        this.estadoModalidad.set(estado);
        this.modalidadSeleccionada.set(estado.idModalidad);
        this.ok.set(`Modalidad registrada: ${estado.modalidad}. Ya puedes redactar tu propuesta.`);
        this.guardandoModalidad.set(false);
        this.cdr.detectChanges();
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'Error al guardar la modalidad.');
        this.guardandoModalidad.set(false);
        this.cdr.detectChanges();
      }
    });
  }

  // ── IA: Analizar ANTES de enviar ──────────────────────────────────────

  /**
   * Llama al endpoint /api/revision-ia/propuesta/previa
   * con los datos del formulario en memoria (SIN guardar en BD).
   */
  analizarConIA(): void {
    if (!this.idEstudiante) {
      this.errorIA.set('No se pudo identificar al estudiante.');
      this.cdr.detectChanges();
      return;
    }
    if (!this.form.titulo.trim()) {
      this.errorIA.set('Escribe al menos el título antes de analizar.');
      this.cdr.detectChanges();
      return;
    }

    this.analizandoIA.set(true);
    this.errorIA.set(null);
    this.feedbackIA.set(null);
    this.respuestaIA.set(null);
    this.cdr.detectChanges();

    this.api.evaluarPropuestaConIAPrevia({
      idEstudiante:          this.idEstudiante,
      titulo:                this.form.titulo,
      temaInvestigacion:     this.form.temaInvestigacion,
      planteamientoProblema: this.form.planteamientoProblema,
      objetivosGenerales:    this.form.objetivosGenerales,
      objetivosEspecificos:  this.form.objetivosEspecificos,
      metodologia:           this.form.metodologia,
      resultadosEsperados:   this.form.resultadosEsperados,
      bibliografia:          this.form.bibliografia,
      modo:                  this.modoIA,
      instruccionAdicional:  this.instruccionIA.trim() || undefined
    })
      .pipe(finalize(() => {
        this.analizandoIA.set(false);
        this.cdr.detectChanges();
      }))
      .subscribe({
        next: resp => {
          this.respuestaIA.set(resp);
          try {
            const parsed: FeedbackIAPropuesta = JSON.parse(resp.feedbackIa);
            this.feedbackIA.set(parsed);
          } catch {
            this.errorIA.set('La IA respondió en formato inesperado. Intente nuevamente.');
          }
          this.cdr.detectChanges();
        },
        error: err => {
          this.errorIA.set(err?.error?.message ?? 'Error al conectarse con el servicio de IA.');
          this.cdr.detectChanges();
        }
      });
  }

  limpiarIA(): void {
    this.feedbackIA.set(null);
    this.respuestaIA.set(null);
    this.errorIA.set(null);
    this.instruccionIA = '';
    this.modoIA = 'integral';
    this.cdr.detectChanges();
  }

  // ── Enviar propuesta (solo después de revisar con IA) ─────────────────

  enviar(): void {
    if (!this.idEstudiante) { this.error.set('No se pudo identificar al estudiante.'); return; }
    if (!this.tieneModalidadSeleccionada) {
      this.error.set('Antes de registrar la propuesta debes seleccionar tu modalidad.'); return;
    }
    if (!this.dentroDePlazoPropuesta) {
      this.error.set(`La propuesta solo puede presentarse entre las semanas 01 y 04 del periodo (semana actual: ${this.semanaActual}).`);
      return;
    }
    if (!this.form.titulo.trim()) { this.error.set('El título es obligatorio.'); return; }

    let bibliografiaFinal = this.form.bibliografia;
    let referenciasFinal: ReferenciaBibliograficaRequest[] | undefined;
    if (this.modalidadEntrega() === 'UPLOAD_PDF') {
      if (!this.urlPdfSubido()) { this.error.set('Sube el PDF de tu propuesta antes de enviar.'); return; }
      bibliografiaFinal = `Documento PDF: ${this.urlPdfSubido()}`;
    } else {
      if (!this.bibliografiaValida) {
        this.error.set(
          `Se requieren al menos 10 referencias bibliográficas, con al menos el 60% de antigüedad ≤ 5 años ` +
          `(tienes ${this.totalReferencias} referencia(s), ${this.porcentajeRecientes}% recientes).`
        );
        return;
      }
      bibliografiaFinal = this.serializarReferencias();
      referenciasFinal = this.referenciasParaBackend();
    }

    this.saving.set(true);
    this.error.set(null);
    this.ok.set(null);

    this.api.crearPropuestaEstudiante(this.idEstudiante, {
      idCarrera:             this.form.idCarrera,
      idTema:                this.form.idTema ?? undefined,
      titulo:                this.form.titulo,
      temaInvestigacion:     this.form.temaInvestigacion,
      planteamientoProblema: this.form.planteamientoProblema,
      objetivosGenerales:    this.form.objetivosGenerales,
      objetivosEspecificos:  this.form.objetivosEspecificos,
      metodologia:           this.form.metodologia,
      resultadosEsperados:   this.form.resultadosEsperados,
      bibliografia:          bibliografiaFinal,
      referencias:           referenciasFinal
    }).subscribe({
      next: () => {
        this.ok.set('✅ Propuesta enviada a la comisión con éxito.');
        this.resetForm();
        this.limpiarIA();
        this.saving.set(false);
        this.cdr.detectChanges();
        this.cargarHistorial();
      },
      error: err => {
        this.error.set(err?.error?.message ?? 'No se pudo enviar la propuesta.');
        this.saving.set(false);
        this.cdr.detectChanges();
        this.cargarEstadoModalidad();
      }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  private resetForm(): void {
    this.form = {
      idCarrera: this.form.idCarrera,
      idTema: null,
      titulo: '',
      temaInvestigacion: '',
      planteamientoProblema: '',
      objetivosGenerales: '',
      objetivosEspecificos: '',
      metodologia: '',
      resultadosEsperados: '',
      bibliografia: ''
    };
    this.referencias = [{ texto: '', anio: null }];
    this.archivoPdf = null;
    this.urlPdfSubido.set(null);
  }

  private normalizarTexto(valor: string | null | undefined): string {
    return (valor ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase();
  }
}
