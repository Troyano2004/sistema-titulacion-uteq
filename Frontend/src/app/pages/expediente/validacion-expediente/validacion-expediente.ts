import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';

import { ExpedienteService, Expediente, DocumentoExpediente } from '../../../services/expediente.service';
import { CatalogosBasicosService, Carrera } from '../../../services/catalogos-basicos.service';
import { getSessionUser } from '../../../services/session';

@Component({
  selector: 'app-validacion-expediente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './validacion-expediente.html',
  styleUrl: './validacion-expediente.scss'
})
export class ValidacionExpedienteComponent implements OnInit {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);

  carreras = signal<Carrera[]>([]);
  expedientes = signal<Expediente[]>([]);
  expedienteSeleccionado = signal<Expediente | null>(null);

  filtroCarrera: number | null = null;
  filtroPeriodo = '';
  filtroEstado = '';

  motivoRechazo: Record<number, string> = {};

  private idUsuario = 0;

  constructor(private expedienteApi: ExpedienteService, private catalogosApi: CatalogosBasicosService) {}

  ngOnInit(): void {
    this.idUsuario = Number(getSessionUser()?.['idUsuario']) || 0;
    this.catalogosApi.listarCarreras().subscribe({ next: d => this.carreras.set(d), error: () => {} });
    this.buscar();
  }

  buscar(): void {
    this.loading.set(true);
    this.expedienteApi.listar(this.filtroCarrera ?? undefined, this.filtroPeriodo || undefined, this.filtroEstado || undefined)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: data => this.expedientes.set(data),
        error: () => this.error.set('No se pudieron cargar los expedientes')
      });
  }

  documentosFaltantes(exp: Expediente): number {
    return exp.documentos.filter(d => d.obligatorio && (d.estado === 'PENDIENTE' || d.estado === 'RECHAZADO')).length;
  }

  verDetalle(exp: Expediente): void {
    this.expedienteApi.obtenerPorId(exp.id).subscribe({
      next: e => this.expedienteSeleccionado.set(e),
      error: () => this.error.set('No se pudo cargar el expediente')
    });
  }

  cerrarDetalle(): void {
    this.expedienteSeleccionado.set(null);
  }

  validar(doc: DocumentoExpediente): void {
    this.expedienteApi.validar(doc.id, this.idUsuario).subscribe({
      next: () => { this.ok.set('Documento validado'); this.refrescarDetalle(); },
      error: (err: any) => this.error.set(err?.error?.message ?? 'No se pudo validar')
    });
  }

  rechazar(doc: DocumentoExpediente): void {
    const motivo = this.motivoRechazo[doc.id];
    if (!motivo || !motivo.trim()) {
      this.error.set('Indica el motivo del rechazo');
      return;
    }
    this.expedienteApi.rechazar(doc.id, this.idUsuario, motivo.trim()).subscribe({
      next: () => { this.ok.set('Documento rechazado'); this.motivoRechazo[doc.id] = ''; this.refrescarDetalle(); },
      error: (err: any) => this.error.set(err?.error?.message ?? 'No se pudo rechazar')
    });
  }

  private refrescarDetalle(): void {
    const exp = this.expedienteSeleccionado();
    if (exp) this.verDetalle(exp);
    this.buscar();
  }
}
