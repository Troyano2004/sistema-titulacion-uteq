import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { finalize } from 'rxjs/operators';

import { ExpedienteService, Expediente, DocumentoExpediente } from '../../../services/expediente.service';
import { DocumentoGeneradorService, PlantillaKey } from '../../../services/documento-generador.service';
import { getSessionEntityId, getSessionUser } from '../../../services/session';

const GENERABLES_TIC: Record<string, PlantillaKey> = {
  A: 'portada', B: 'autoria', C: 'certificacion-director', D: 'certificado-antiplagio',
  E: 'pagina-tribunal', F: 'codigo-dublin', G: 'informe-favorable', H: 'certificacion-tribunal',
  I: 'certificado-redaccion', J: 'acta-sustentacion', K: 'acta-grado',
};
const GENERABLES_COMPLEXIVO: Record<string, PlantillaKey> = { I: 'acta-grado' };

@Component({
  selector: 'app-expediente-estudiante',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './expediente-estudiante.html',
  styleUrl: './expediente-estudiante.scss'
})
export class ExpedienteEstudianteComponent implements OnInit {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);
  generandoCodigo = signal<string | null>(null);
  subiendoId = signal<number | null>(null);

  expediente = signal<Expediente | null>(null);

  modalidadNueva: 'TIC' | 'COMPLEXIVO' = 'TIC';
  periodoNuevo = '';

  private idEstudiante = 0;

  progreso = computed(() => {
    const e = this.expediente();
    if (!e) return 0;
    const aprobados = e.documentos.filter(d => d.estado === 'VALIDADO' || d.estado === 'APROBADO').length;
    return e.documentos.length ? Math.round((aprobados / e.documentos.length) * 100) : 0;
  });

  constructor(
    private expedienteApi: ExpedienteService,
    private generadorApi: DocumentoGeneradorService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    const user = getSessionUser();
    this.idEstudiante = getSessionEntityId(user, 'estudiante') ?? 0;
    this.cargar();
  }

  private cargar(): void {
    if (!this.idEstudiante) return;
    this.loading.set(true);
    this.expedienteApi.obtenerPorEstudiante(this.idEstudiante).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: e => this.expediente.set(e),
      error: () => this.expediente.set(null)
    });
  }

  crearExpediente(): void {
    if (!this.periodoNuevo.trim()) { this.error.set('Indica el periodo de titulación'); return; }
    this.loading.set(true);
    this.expedienteApi.crear(this.idEstudiante, this.modalidadNueva, this.periodoNuevo.trim())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: e => { this.expediente.set(e); this.ok.set('Expediente creado'); },
        error: (err: any) => this.error.set(err?.error?.message ?? 'Error al crear el expediente')
      });
  }

  esGenerable(doc: DocumentoExpediente): PlantillaKey | null {
    const e = this.expediente();
    if (!e) return null;
    const mapa = e.modalidad === 'TIC' ? GENERABLES_TIC : GENERABLES_COMPLEXIVO;
    return mapa[doc.codigo] ?? null;
  }

  generar(doc: DocumentoExpediente): void {
    const plantilla = this.esGenerable(doc);
    if (!plantilla) return;
    this.generandoCodigo.set(doc.codigo);
    this.generadorApi.generar(plantilla, this.idEstudiante)
      .pipe(finalize(() => this.generandoCodigo.set(null)))
      .subscribe({
        next: blob => {
          this.descargar(blob, `${plantilla}-${this.idEstudiante}.pdf`);
          this.ok.set('Documento generado y registrado en el expediente');
          this.cargar();
        },
        error: (err: any) => this.error.set(err?.error?.message ?? 'No se pudo generar el documento')
      });
  }

  subirArchivo(doc: DocumentoExpediente, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);
    this.subiendoId.set(doc.id);
    this.http.post<{ url: string }>('http://localhost:8080/api/uploads/files', formData)
      .pipe(finalize(() => this.subiendoId.set(null)))
      .subscribe({
        next: resp => {
          this.expedienteApi.cargar(doc.id, resp.url).subscribe({
            next: () => { this.ok.set('Archivo cargado'); this.cargar(); },
            error: () => this.error.set('No se pudo registrar el archivo en el expediente')
          });
        },
        error: () => this.error.set('No se pudo subir el archivo')
      });
    input.value = '';
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
