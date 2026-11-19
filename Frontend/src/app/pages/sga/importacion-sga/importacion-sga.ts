import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs/operators';

import { SgaImportService, ImportacionResultado, ImportacionLog } from '../../../services/sga-import.service';
import { getSessionUser } from '../../../services/session';

type TipoImport = 'pensum' | 'requisitos' | 'deudas';

@Component({
  selector: 'app-importacion-sga',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './importacion-sga.html',
  styleUrl: './importacion-sga.scss'
})
export class ImportacionSgaComponent implements OnInit {
  subiendo = signal<TipoImport | null>(null);
  error = signal<string | null>(null);
  resultados = signal<Record<TipoImport, ImportacionResultado | null>>({ pensum: null, requisitos: null, deudas: null });
  historial = signal<ImportacionLog[]>([]);

  private idUsuario: number | undefined;

  constructor(private sgaApi: SgaImportService) {}

  ngOnInit(): void {
    const idUsuario = Number(getSessionUser()?.['idUsuario']);
    this.idUsuario = Number.isFinite(idUsuario) && idUsuario > 0 ? idUsuario : undefined;
    this.cargarHistorial();
  }

  onArchivoSeleccionado(tipo: TipoImport, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.subiendo.set(tipo);
    this.error.set(null);

    const obs = tipo === 'pensum' ? this.sgaApi.importarPensum(file, this.idUsuario)
      : tipo === 'requisitos' ? this.sgaApi.importarRequisitos(file, this.idUsuario)
      : this.sgaApi.importarDeudas(file, this.idUsuario);

    obs.pipe(finalize(() => this.subiendo.set(null))).subscribe({
      next: resultado => {
        this.resultados.update(r => ({ ...r, [tipo]: resultado }));
        this.cargarHistorial();
      },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al importar el archivo')
    });
    input.value = '';
  }

  private cargarHistorial(): void {
    this.sgaApi.historial().subscribe({ next: h => this.historial.set(h), error: () => {} });
  }
}
