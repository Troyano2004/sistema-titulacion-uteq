import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';
import { firstValueFrom } from 'rxjs';
import {
  Dt2Service,
  ProyectoPendienteConfiguracionDto,
  CertificadoAntiplacioDto,
  DocumentoTitulacionTextoDto,
  GuardarRevisionIaRequest
} from '../../../services/dt2.service';
import { AntiplagioCompilatioService, CertificadoCompilatioResponse } from '../../../services/antiplagio-compilatio.service';
import { getSessionUser, getSessionEntityId } from '../../../services/session';

// ── Tipos locales para el análisis IA ─────────────────────────────────────────

export interface WasItAIResult {
  isAI: boolean;
  confidence: number;
  patterns: string[];
  analysis: { likelihood: string; reasoning: string };
  sentences: { text: string; isAI: boolean; confidence: number; scores: { ai: number; human: number } }[];
}

export interface SeccionAnalisis {
  clave: string;
  label: string;
  texto: string;
  resultado: WasItAIResult | null;
  analizando: boolean;
  error: string | null;
}

/** Mapa de campos del documento → etiqueta visible */
const SECCIONES_CONFIG: { clave: keyof DocumentoTitulacionTextoDto; label: string }[] = [
  { clave: 'resumen',               label: 'Resumen' },
  { clave: 'abstractText',          label: 'Abstract' },
  { clave: 'introduccion',          label: 'Introducción' },
  { clave: 'planteamientoProblema', label: 'Planteamiento del Problema' },
  { clave: 'justificacion',         label: 'Justificación' },
  { clave: 'objetivoGeneral',       label: 'Objetivo General' },
  { clave: 'objetivosEspecificos',  label: 'Objetivos Específicos' },
  { clave: 'marcoTeorico',          label: 'Marco Teórico' },
  { clave: 'metodologia',           label: 'Metodología' },
  { clave: 'resultados',            label: 'Resultados' },
  { clave: 'discusion',             label: 'Discusión' },
  { clave: 'conclusiones',          label: 'Conclusiones' },
  { clave: 'recomendaciones',       label: 'Recomendaciones' },
];

/** Umbral: si el texto empieza con esto se considera "sin contenido real" */
const PLACEHOLDER_PREFIXES = ['pendiente', 'por definir', 'sin definir'];

function esPlaceholder(texto: string): boolean {
  const t = texto.trim().toLowerCase();
  return PLACEHOLDER_PREFIXES.some(p => t.startsWith(p));
}

@Component({
  selector: 'app-antiplagio-dt2',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './antiplagio-dt2.html',
  styleUrl: './antiplagio-dt2.scss'
})
export class AntiplagioDt2Component implements OnInit {

  // ── Estado general ──────────────────────────────────────────────────────────
  loading        = signal(false);
  error          = signal<string | null>(null);
  ok             = signal<string | null>(null);

  // ── Proyectos y detalle ─────────────────────────────────────────────────────
  proyectos            = signal<ProyectoPendienteConfiguracionDto[]>([]);
  proyectoSeleccionado = signal<ProyectoPendienteConfiguracionDto | null>(null);
  certificado          = signal<CertificadoAntiplacioDto | null>(null);

  // ── Documento de titulación (secciones de texto) ────────────────────────────
  documentoTexto    = signal<DocumentoTitulacionTextoDto | null>(null);
  cargandoDocumento = signal(false);

  // ── Análisis IA ─────────────────────────────────────────────────────────────
  analizandoIA       = signal(false);
  guardandoIA        = signal(false);
  seccionesAnalizadas = signal<SeccionAnalisis[]>([]);
  promedioConfianzaIA = signal<number | null>(null);

  // ── Tabs ────────────────────────────────────────────────────────────────────
  tabActiva = signal<'compilatio' | 'ia'>('compilatio');

  // ── Formulario COMPILATIO ───────────────────────────────────────────────────
  archivoSeleccionado: File | null = null;
  formAntiplagio: FormGroup;

  // ── Emisión del certificado (solo si el último porcentaje < 10%) ───────────
  emitiendoCertificado = signal(false);
  certificadoEmitido = signal<CertificadoCompilatioResponse | null>(null);

  // ── Análisis IA: el backend hace de proxy hacia WasItAIGenerated ────────────
  // La API key ya no vive en el frontend — ver AntiplagioController /
  // AntiplagioIaService en el backend (variable de entorno WASITAI_API_KEY).
  private readonly ANTIPLAGIO_IA_ENDPOINT = 'http://localhost:8080/api/antiplagio/verificar-texto';

  private idDocenteDt2 = 0;

  constructor(
    private dt2: Dt2Service,
    private fb: FormBuilder,
    private http: HttpClient,
    private compilatioApi: AntiplagioCompilatioService,
  ) {
    this.formAntiplagio = this.fb.group({
      porcentajeCoincidencia: [null, [Validators.required, Validators.min(0), Validators.max(100)]],
      observaciones: ['']
    });
  }

  ngOnInit(): void {
    const user = getSessionUser();
    this.idDocenteDt2 = getSessionEntityId(user, 'docente') ?? 0;
    this.cargarProyectos();
  }

  // ── Selección de proyecto ───────────────────────────────────────────────────

  seleccionarProyecto(p: ProyectoPendienteConfiguracionDto): void {
    this.proyectoSeleccionado.set(p);
    this.error.set(null);
    this.ok.set(null);
    this.archivoSeleccionado = null;
    this.formAntiplagio.reset();
    this.seccionesAnalizadas.set([]);
    this.promedioConfianzaIA.set(null);
    this.documentoTexto.set(null);
    this.certificadoEmitido.set(null);
    this.tabActiva.set('compilatio');
    this.cargarCertificado(p.idProyecto);
    this.cargarDocumentoTexto(p.idProyecto);
  }

  /** Solo permitido si el último porcentaje registrado es < 10% (favorable). */
  get puedeEmitirCertificado(): boolean {
    return Boolean(this.certificado()?.certificadoFavorable) && !this.certificadoEmitido();
  }

  emitirCertificado(): void {
    const p = this.proyectoSeleccionado();
    if (!p?.idEstudiante || !this.puedeEmitirCertificado) return;

    this.emitiendoCertificado.set(true);
    this.error.set(null);
    this.compilatioApi.emitirCertificado(p.idEstudiante)
      .pipe(finalize(() => this.emitiendoCertificado.set(false)))
      .subscribe({
        next: cert => {
          this.certificadoEmitido.set(cert);
          this.ok.set('✓ Certificado antiplagio emitido y firmado por el Director.');
        },
        error: (err: any) => this.error.set(
          err?.error?.message ?? err?.error?.mensaje ?? 'No se pudo emitir el certificado'
        )
      });
  }

  cambiarTab(tab: 'compilatio' | 'ia'): void {
    this.tabActiva.set(tab);
  }

  // ── COMPILATIO ──────────────────────────────────────────────────────────────

  onArchivoChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const f = input.files[0];
    if (f.type !== 'application/pdf') {
      this.error.set('Solo se permiten archivos PDF.');
      this.archivoSeleccionado = null;
      input.value = '';
      return;
    }
    if (f.size > 50 * 1024 * 1024) {
      this.error.set('El archivo no puede superar 50 MB.');
      this.archivoSeleccionado = null;
      input.value = '';
      return;
    }
    this.archivoSeleccionado = f;
    this.error.set(null);
  }

  subirAntiplagio(): void {
    const p = this.proyectoSeleccionado();
    if (!p) return;
    if (!this.archivoSeleccionado) {
      this.error.set('Debes seleccionar el informe COMPILATIO en PDF.');
      return;
    }
    if (this.formAntiplagio.invalid) {
      this.formAntiplagio.markAllAsTouched();
      return;
    }

    const v  = this.formAntiplagio.value;
    const fd = new FormData();
    fd.append('archivo', this.archivoSeleccionado);
    fd.append('idDocenteDt2', String(this.idDocenteDt2));
    fd.append('porcentajeCoincidencia', String(v.porcentajeCoincidencia));
    if (v.observaciones) fd.append('observaciones', v.observaciones);

    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);

    this.dt2.registrarAntiplagio(p.idProyecto, fd)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: cert => {
          this.certificado.set(cert);
          const estado = cert.certificadoFavorable
            ? '✓ FAVORABLE — el documento pasa a ANTIPLAGIO_APROBADO. El coordinador puede programar la predefensa.'
            : '✗ RECHAZADO — porcentaje ≥ 10 %. Debe corregir el documento y volver a subir el informe.';
          this.ok.set(`Informe registrado. ${estado}`);
          this.archivoSeleccionado = null;
          this.formAntiplagio.reset();
          this.cargarProyectos();
        },
        error: (err: any) => this.error.set(
          err?.error?.mensaje ?? err?.error?.message ?? 'Error al registrar informe antiplagio'
        )
      });
  }

  // ── Análisis IA ─────────────────────────────────────────────────────────────

  async analizarConIA(): Promise<void> {
    const doc = this.documentoTexto();
    if (!doc) return;

    // Filtrar secciones con contenido real
    const conTexto = SECCIONES_CONFIG
      .map(cfg => ({
        clave: cfg.clave,
        label: cfg.label,
        texto: (doc[cfg.clave] as string | null) ?? ''
      }))
      .filter(s => s.texto && s.texto.trim().length > 50 && !esPlaceholder(s.texto));

    if (conTexto.length === 0) {
      this.error.set('El documento no tiene secciones con contenido suficiente para analizar. Solicita al estudiante que complete el documento.');
      return;
    }

    // Inicializar estado "analizando" para cada sección
    this.seccionesAnalizadas.set(
      conTexto.map(s => ({ ...s, resultado: null, analizando: true, error: null }))
    );
    this.analizandoIA.set(true);
    this.promedioConfianzaIA.set(null);
    this.error.set(null);

    // Llamar a la API sección por sección (secuencial → no satura la API)
    for (let i = 0; i < conTexto.length; i++) {
      try {
        const resultado = await this.llamarWasItAI(conTexto[i].texto);
        this.seccionesAnalizadas.update(secciones =>
          secciones.map((s, idx) => idx === i ? { ...s, resultado, analizando: false } : s)
        );
      } catch (err: any) {
        this.seccionesAnalizadas.update(secciones =>
          secciones.map((s, idx) =>
            idx === i ? { ...s, error: err?.message ?? 'Error al analizar', analizando: false } : s
          )
        );
      }
    }

    this.analizandoIA.set(false);
    this.calcularPromedio();
  }

  /** Guarda el resultado del análisis IA en el backend */
  guardarResultadoIA(): void {
    const p = this.proyectoSeleccionado();
    const prom = this.promedioConfianzaIA();
    if (!p || prom === null) return;

    const nivel = this.getNivelRiesgo();

    // Construir resumen por sección en texto plano
    const detalle = this.seccionesAnalizadas()
      .filter(s => s.resultado)
      .map(s => `${s.label}: ${(s.resultado!.confidence * 100).toFixed(1)}% IA (${s.resultado!.isAI ? 'IA' : 'Humano'}) — ${s.resultado!.analysis.reasoning}`)
      .join(' | ');

    const estadoRevisionIa = nivel === 'bajo' ? 'LIMPIO' : nivel === 'medio' ? 'SOSPECHOSO' : 'ANALIZADO';

    const req: GuardarRevisionIaRequest = {
      porcentajePromedioIa: prom,
      nivelRiesgo: nivel.toUpperCase(),
      feedbackIa: detalle,
      estadoRevisionIa
    };

    this.guardandoIA.set(true);
    this.dt2.guardarRevisionIa(p.idProyecto, req)
      .pipe(finalize(() => this.guardandoIA.set(false)))
      .subscribe({
        next: msg => {
          this.ok.set(`✓ ${msg.mensaje}`);
          // Refrescar el documento para mostrar estadoRevisionIa actualizado
          this.cargarDocumentoTexto(p.idProyecto);
        },
        error: (err: any) => this.error.set(
          err?.error?.mensaje ?? err?.error?.message ?? 'Error al guardar el análisis IA'
        )
      });
  }

  // ── Helpers de UI ───────────────────────────────────────────────────────────

  getNivelRiesgo(): 'bajo' | 'medio' | 'alto' {
    const p = this.promedioConfianzaIA();
    if (p === null) return 'bajo';
    if (p < 40)    return 'bajo';
    if (p < 70)    return 'medio';
    return 'alto';
  }

  getBarraColor(confidence: number): string {
    const pct = confidence * 100;
    if (pct < 40) return '#38a169';
    if (pct < 70) return '#d69e2e';
    return '#e53e3e';
  }

  // ── Privados ────────────────────────────────────────────────────────────────

  private async llamarWasItAI(texto: string): Promise<WasItAIResult> {
    try {
      return await firstValueFrom(
        this.http.post<WasItAIResult>(this.ANTIPLAGIO_IA_ENDPOINT, { texto })
      );
    } catch (err: any) {
      const mensaje = err?.error?.message ?? err?.message ?? `HTTP ${err?.status ?? '?'}`;
      throw new Error(mensaje);
    }
  }

  private calcularPromedio(): void {
    const con = this.seccionesAnalizadas().filter(s => s.resultado !== null);
    if (!con.length) return;
    const suma = con.reduce((acc, s) => acc + s.resultado!.confidence * 100, 0);
    this.promedioConfianzaIA.set(Math.round(suma / con.length));
  }

  private cargarProyectos(): void {
    this.loading.set(true);
    this.dt2.listarProyectosDocenteDt2(this.idDocenteDt2)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: data => this.proyectos.set(data),
        error: ()   => this.error.set('Error al cargar proyectos')
      });
  }

  private cargarCertificado(idProyecto: number): void {
    this.dt2.getCertificado(idProyecto).subscribe({
      next: data => this.certificado.set(data),
      error: ()   => this.certificado.set(null)
    });
  }

  /**
   * Llama a GET /api/dt2/proyectos/{idProyecto}/documento-texto
   * usando el método getDocumentoTexto() añadido al Dt2Service Angular.
   */
  private cargarDocumentoTexto(idProyecto: number): void {
    this.cargandoDocumento.set(true);
    this.dt2.getDocumentoTexto(idProyecto)
      .pipe(finalize(() => this.cargandoDocumento.set(false)))
      .subscribe({
        next: doc => this.documentoTexto.set(doc),
        error: ()  => this.documentoTexto.set(null)
      });
  }
}
