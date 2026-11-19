import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { QuillEditorComponent } from 'ngx-quill';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { finalize } from 'rxjs/operators';

import {
  DocumentoTicService,
  DocumentoTic,
  SeccionTic,
  VersionTic,
  VersionTicDetalle,
  ComentarioTic,
  RolAutorComentarioTic
} from '../../../services/documento-tic.service';
import { CatalogosBasicosService, TipoTrabajoTitulacion } from '../../../services/catalogos-basicos.service';
import { getSessionEntityId, getSessionUser, hasAnyRole } from '../../../services/session';

const AUTOSAVE_MS = 30000;

@Component({
  selector: 'app-editor-tic',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, QuillEditorComponent],
  templateUrl: './editor-tic.html',
  styleUrl: './editor-tic.scss'
})
export class EditorTicComponent implements OnInit, OnDestroy {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);

  documento = signal<DocumentoTic | null>(null);
  seccionActual = signal<SeccionTic | null>(null);
  contenidoEditando = '';
  guardandoSeccion = signal(false);
  huboCambiosSinGuardar = false;

  comentarios = signal<ComentarioTic[]>([]);
  panelComentariosAbierto = signal(true);
  nuevoComentario = '';

  versiones = signal<VersionTic[]>([]);
  mostrarHistorial = signal(false);
  versionSeleccionada = signal<VersionTicDetalle | null>(null);

  tiposTrabajos = signal<TipoTrabajoTitulacion[]>([]);
  formCreacion: FormGroup;

  archivoSubiendo = signal(false);

  esDirectorOTribunal = hasAnyRole('DOCENTE', 'DOCENTE_TITULADO', 'ADMIN');
  private idEstudiante = 0;
  private idUsuario = 0;
  private nombreUsuario = 'Usuario';
  private autoSaveTimer: any = null;

  progreso = computed(() => {
    const doc = this.documento();
    if (!doc) return '';
    return `${doc.seccionesCompletas}/${doc.seccionesTotales} secciones completas`;
  });

  constructor(
    private api: DocumentoTicService,
    private catalogosApi: CatalogosBasicosService,
    private fb: FormBuilder,
    private http: HttpClient,
    private sanitizer: DomSanitizer,
    private route: ActivatedRoute
  ) {
    this.formCreacion = this.fb.group({
      idTipoTrabajo: [null, Validators.required],
      modalidadEntrega: ['REDACCION_SISTEMA', Validators.required],
      titulo: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    const user = getSessionUser();
    this.idEstudiante = getSessionEntityId(user, 'estudiante') ?? 0;
    this.idUsuario = Number(user?.['idUsuario']) || 0;
    this.nombreUsuario = ((user?.['nombres'] as string) ?? '') + ' ' + ((user?.['apellidos'] as string) ?? '');
    if (!this.nombreUsuario.trim()) this.nombreUsuario = 'Usuario';

    this.catalogosApi.listarTiposTrabajo().subscribe({ next: d => this.tiposTrabajos.set(d), error: () => {} });

    const idParam = Number(this.route.snapshot.paramMap.get('id'));
    if (idParam) {
      this.cargarDocumentoPorId(idParam); // director/tribunal revisando un documento específico
    } else {
      this.cargarDocumentoExistente(); // estudiante: su propio documento
    }
  }

  ngOnDestroy(): void {
    this.detenerAutoguardado();
  }

  private cargarDocumentoPorId(idDocumento: number): void {
    this.loading.set(true);
    this.api.obtener(idDocumento).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: doc => {
        this.documento.set(doc);
        this.cargarComentarios(doc.id);
        if (doc.secciones.length) this.seleccionarSeccion(doc.secciones[0]);
      },
      error: () => this.error.set('No se pudo cargar el documento')
    });
  }

  private cargarDocumentoExistente(): void {
    if (!this.idEstudiante) return;
    this.loading.set(true);
    this.api.obtenerPorEstudiante(this.idEstudiante).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: doc => {
        this.documento.set(doc);
        this.cargarComentarios(doc.id);
        if (doc.secciones.length) this.seleccionarSeccion(doc.secciones[0]);
      },
      error: () => this.documento.set(null) // 404 -> mostrar formulario de creación
    });
  }

  crearDocumento(): void {
    if (this.formCreacion.invalid) {
      this.formCreacion.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    const v = this.formCreacion.value;
    this.api.crear({
      idEstudiante: this.idEstudiante,
      idTipoTrabajo: v.idTipoTrabajo,
      modalidadEntrega: v.modalidadEntrega,
      titulo: v.titulo
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: doc => {
        this.documento.set(doc);
        if (doc.secciones.length) this.seleccionarSeccion(doc.secciones[0]);
      },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al crear el documento')
    });
  }

  // ── Navegación de secciones ─────────────────────────────────────────
  seleccionarSeccion(seccion: SeccionTic): void {
    this.guardarSeccionActual(true);
    this.seccionActual.set(seccion);
    this.contenidoEditando = seccion.contenidoHtml ?? '';
    this.huboCambiosSinGuardar = false;
    this.cargarComentariosDeSeccion();
    this.iniciarAutoguardado();
  }

  onContenidoCambia(html: string): void {
    this.contenidoEditando = html;
    this.huboCambiosSinGuardar = true;
  }

  private iniciarAutoguardado(): void {
    this.detenerAutoguardado();
    this.autoSaveTimer = setInterval(() => {
      if (this.huboCambiosSinGuardar) this.guardarSeccionActual(false);
    }, AUTOSAVE_MS);
  }

  private detenerAutoguardado(): void {
    if (this.autoSaveTimer) { clearInterval(this.autoSaveTimer); this.autoSaveTimer = null; }
  }

  guardarSeccionActual(silencioso = false): void {
    const doc = this.documento();
    const seccion = this.seccionActual();
    if (!doc || !seccion || seccion.autoLlenada || !this.huboCambiosSinGuardar) return;

    this.guardandoSeccion.set(true);
    this.api.guardarSeccion(doc.id, seccion.codigoSeccion, this.contenidoEditando)
      .pipe(finalize(() => this.guardandoSeccion.set(false)))
      .subscribe({
        next: actualizada => {
          this.huboCambiosSinGuardar = false;
          this.actualizarSeccionLocal(actualizada);
          if (!silencioso) this.ok.set('Sección guardada');
        },
        error: (err: any) => this.error.set(err?.error?.message ?? 'Error al guardar la sección')
      });
  }

  private actualizarSeccionLocal(actualizada: SeccionTic): void {
    const doc = this.documento();
    if (!doc) return;
    const secciones = doc.secciones.map(s => s.codigoSeccion === actualizada.codigoSeccion ? actualizada : s);
    const completas = secciones.filter(s => s.estado === 'COMPLETO').length;
    this.documento.set({ ...doc, secciones, seccionesCompletas: completas });
    this.seccionActual.set(actualizada);
  }

  // ── Versiones (explícitas — nunca automáticas) ──────────────────────
  crearVersion(): void {
    const doc = this.documento();
    if (!doc) return;
    this.guardarSeccionActual(true);
    this.api.crearVersion(doc.id, this.nombreUsuario.trim()).subscribe({
      next: () => this.ok.set('Versión creada correctamente'),
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al crear la versión')
    });
  }

  verHistorial(): void {
    const doc = this.documento();
    if (!doc) return;
    this.api.listarVersiones(doc.id).subscribe({
      next: v => { this.versiones.set(v); this.mostrarHistorial.set(true); },
      error: () => this.error.set('No se pudo cargar el historial de versiones')
    });
  }

  verVersion(idVersion: number): void {
    const doc = this.documento();
    if (!doc) return;
    this.api.obtenerVersion(doc.id, idVersion).subscribe({
      next: v => this.versionSeleccionada.set(v),
      error: () => this.error.set('No se pudo cargar la versión')
    });
  }

  cerrarHistorial(): void {
    this.mostrarHistorial.set(false);
    this.versionSeleccionada.set(null);
  }

  // ── Comentarios ──────────────────────────────────────────────────────
  private cargarComentarios(idDocumento: number): void {
    this.api.listarComentarios(idDocumento).subscribe({ next: c => this.comentarios.set(c), error: () => {} });
  }

  get comentariosDeSeccionActual(): ComentarioTic[] {
    const seccion = this.seccionActual();
    if (!seccion) return this.comentarios().filter(c => c.idSeccion === null);
    return this.comentarios().filter(c => c.idSeccion === seccion.id);
  }

  private cargarComentariosDeSeccion(): void {
    const doc = this.documento();
    if (doc) this.cargarComentarios(doc.id);
  }

  agregarComentario(): void {
    const doc = this.documento();
    if (!doc || !this.nuevoComentario.trim()) return;
    const rol: RolAutorComentarioTic = 'DIRECTOR'; // simplificación: el rol exacto (presidente/vocal) se define en tribunal
    this.api.agregarComentario(doc.id, this.seccionActual()?.id ?? null, this.idUsuario, rol, this.nuevoComentario.trim())
      .subscribe({
        next: c => { this.comentarios.set([...this.comentarios(), c]); this.nuevoComentario = ''; },
        error: (err: any) => this.error.set(err?.error?.message ?? 'Error al agregar el comentario')
      });
  }

  resolverComentario(idComentario: number): void {
    const doc = this.documento();
    if (!doc) return;
    this.api.resolverComentario(doc.id, idComentario).subscribe({
      next: actualizado => this.comentarios.set(this.comentarios().map(c => c.id === actualizado.id ? actualizado : c)),
      error: () => this.error.set('No se pudo resolver el comentario')
    });
  }

  // ── Modalidad UPLOAD_PDF ─────────────────────────────────────────────
  onArchivoSeleccionado(event: Event): void {
    const doc = this.documento();
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!doc || !file) return;

    const formData = new FormData();
    formData.append('file', file);
    this.archivoSubiendo.set(true);
    this.http.post<{ url: string }>('http://localhost:8080/api/uploads/files', formData)
      .pipe(finalize(() => this.archivoSubiendo.set(false)))
      .subscribe({
        next: resp => {
          this.api.registrarArchivoPdf(doc.id, resp.url).subscribe({
            next: actualizado => { this.documento.set(actualizado); this.ok.set('PDF subido correctamente'); },
            error: () => this.error.set('No se pudo registrar el PDF en el documento')
          });
        },
        error: () => this.error.set('No se pudo subir el archivo PDF')
      });
    input.value = '';
  }

  // ── Exportación ──────────────────────────────────────────────────────
  exportarPdf(): void {
    const doc = this.documento();
    if (!doc) return;
    this.api.exportarPdf(doc.id).subscribe({
      next: blob => this.descargar(blob, `documento-${doc.id}.pdf`),
      error: () => this.error.set('No se pudo exportar el PDF')
    });
  }

  exportarDocx(): void {
    const doc = this.documento();
    if (!doc) return;
    this.api.exportarDocx(doc.id).subscribe({
      next: blob => this.descargar(blob, `documento-${doc.id}.docx`),
      error: () => this.error.set('No se pudo exportar el DOCX')
    });
  }

  urlSegura(url: string | undefined): SafeResourceUrl {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url ?? '');
  }

  private descargar(blob: Blob, nombre: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombre;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
