import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocentesService, DocenteConCarreras, DocenteSimple } from '../../../services/docentes.service';
import { CatalogoCarrera, CatalogosService } from '../../../services/catalogos';
import { SelectorDocenteComponent } from '../../../shared/selector-docente/selector-docente.component';

@Component({
  selector: 'app-docentes-carreras',
  standalone: true,
  imports: [CommonModule, FormsModule, SelectorDocenteComponent],
  template: `
    <section class="page">
      <div class="page-header">
        <div>
          <h1>Docentes y Carreras</h1>
          <p class="subtitle">Vincula cada docente a la(s) carrera(s) a la(s) que pertenece — esto es lo que permite, por ejemplo, que se le auto-seleccione la carrera al registrar un tema.</p>
        </div>
        <button class="btn-primary" (click)="selector.abrir()">+ Vincular docente a carrera</button>
      </div>

      <app-selector-docente #selector (seleccionar)="onDocenteElegido($event)" />

      <div class="panel-vincular" *ngIf="docenteEnVinculo">
        <div class="panel-vincular-info">
          <strong>Vincular a:</strong> {{ docenteEnVinculo.nombreCompleto }} &middot; {{ docenteEnVinculo.cedula || 'sin cédula' }}
        </div>
        <select [(ngModel)]="carreraParaVincular">
          <option [ngValue]="null">Selecciona una carrera...</option>
          <option *ngFor="let c of carreras" [ngValue]="c.idCarrera">{{ c.nombre }}</option>
        </select>
        <button class="btn-primary sm" [disabled]="!carreraParaVincular || vinculando" (click)="vincular()">
          {{ vinculando ? 'Vinculando...' : 'Vincular' }}
        </button>
        <button class="btn-secondary sm" (click)="cancelarVinculo()">Cancelar</button>
      </div>

      <div class="alert alert-danger" *ngIf="error">{{ error }}</div>
      <div class="alert alert-success" *ngIf="mensaje">{{ mensaje }}</div>

      <div class="search-box">
        <input type="text" [(ngModel)]="filtro" placeholder="Buscar por nombre o cédula...">
      </div>

      <div class="alert alert-info" *ngIf="loading">Cargando...</div>

      <div class="table-container" *ngIf="!loading">
        <table>
          <thead>
            <tr>
              <th>Docente</th>
              <th>Cédula</th>
              <th>Carreras vinculadas</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let d of docentesFiltrados">
              <td>{{ d.nombreCompleto }}</td>
              <td>{{ d.cedula || '-' }}</td>
              <td>
                <span class="chip" *ngFor="let c of d.carreras">
                  {{ c.nombre }}
                  <button class="chip-x" (click)="quitar(d, c)" [disabled]="quitando === (d.idDocente + '-' + c.idCarrera)" title="Quitar">&times;</button>
                </span>
                <span class="sin-carreras" *ngIf="d.carreras.length === 0">Sin carreras vinculadas</span>
              </td>
            </tr>
            <tr *ngIf="docentesFiltrados.length === 0">
              <td colspan="3" class="text-center">No hay docentes que coincidan con el filtro</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  `,
  styles: [`
    .page { padding: 1.5rem; }
    .page-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
    .page-header h1 { margin: 0 0 .35rem; }
    .subtitle { margin: 0; color: #6b7280; font-size: .875rem; max-width: 640px; }
    .btn-primary { background: #0f7a3a; color: white; padding: 0.5rem 1rem; border: none; border-radius: 6px; cursor: pointer; white-space: nowrap; }
    .btn-primary:disabled { opacity: .5; cursor: not-allowed; }
    .btn-primary.sm { padding: .35rem .75rem; font-size: .85rem; }
    .btn-secondary { background: #6b7280; color: white; padding: 0.5rem 1rem; border: none; border-radius: 6px; cursor: pointer; }
    .btn-secondary.sm { padding: .35rem .75rem; font-size: .85rem; }

    .panel-vincular {
      display: flex; align-items: center; gap: .75rem; flex-wrap: wrap;
      background: #f0fff4; border: 1px solid #c6f6d5; border-radius: 8px;
      padding: .75rem 1rem; margin-bottom: 1rem;
    }
    .panel-vincular-info { font-size: .88rem; color: #276749; }
    .panel-vincular select { padding: .4rem .6rem; border: 1px solid #d1d5db; border-radius: 6px; }

    .search-box { margin-bottom: 1rem; }
    .search-box input { width: 100%; max-width: 360px; padding: .55rem .75rem; border: 1px solid #d1d5db; border-radius: 6px; box-sizing: border-box; }

    .table-container { background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    table { width: 100%; border-collapse: collapse; }
    th { background: #f9fafb; padding: 0.75rem; text-align: left; font-weight: 600; }
    td { padding: 0.75rem; border-top: 1px solid #e5e7eb; vertical-align: middle; }
    .text-center { text-align: center; color: #6b7280; }

    .chip {
      display: inline-flex; align-items: center; gap: .3rem;
      background: #e9f6ef; color: #0c6a32; border-radius: 999px;
      padding: .2rem .3rem .2rem .65rem; font-size: .8rem; font-weight: 600;
      margin: .15rem .3rem .15rem 0;
    }
    .chip-x {
      background: none; border: none; color: #0c6a32; cursor: pointer;
      width: 18px; height: 18px; border-radius: 50%; font-size: .95rem; line-height: 1;
      display: grid; place-items: center;
    }
    .chip-x:hover { background: rgba(12,106,50,0.15); }
    .chip-x:disabled { opacity: .4; cursor: not-allowed; }
    .sin-carreras { color: #9ca3af; font-size: .82rem; font-style: italic; }

    .alert { padding: 1rem; border-radius: 6px; margin-bottom: 1rem; }
    .alert-info { background: #dbeafe; color: #1e40af; }
    .alert-danger { background: #fee2e2; color: #991b1b; }
    .alert-success { background: #d1fae5; color: #065f46; }
  `]
})
export class DocentesCarrerasComponent implements OnInit {
  docentes: DocenteConCarreras[] = [];
  carreras: CatalogoCarrera[] = [];
  filtro = '';
  loading = false;
  error = '';
  mensaje = '';

  docenteEnVinculo: DocenteSimple | null = null;
  carreraParaVincular: number | null = null;
  vinculando = false;
  quitando: string | null = null;

  constructor(
    private readonly docentesApi: DocentesService,
    private readonly catalogosApi: CatalogosService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.catalogosApi.listarCarreras().subscribe({
      next: data => { this.carreras = data ?? []; this.cdr.detectChanges(); },
      error: () => { /* el panel de vincular queda sin opciones; no bloquea la tabla */ }
    });
  }

  get docentesFiltrados(): DocenteConCarreras[] {
    const q = this.filtro.trim().toLowerCase();
    if (!q) return this.docentes;
    return this.docentes.filter(d =>
      d.nombreCompleto.toLowerCase().includes(q) ||
      (d.cedula ?? '').toLowerCase().includes(q) ||
      d.carreras.some(c => c.nombre.toLowerCase().includes(q))
    );
  }

  cargar(): void {
    this.loading = true;
    this.error = '';
    this.docentesApi.listarConCarreras().subscribe({
      next: data => { this.docentes = data ?? []; this.loading = false; this.cdr.detectChanges(); },
      error: () => { this.error = 'No se pudo cargar la lista de docentes.'; this.loading = false; this.cdr.detectChanges(); }
    });
  }

  onDocenteElegido(d: DocenteSimple): void {
    this.docenteEnVinculo = d;
    this.carreraParaVincular = null;
    this.error = '';
    this.mensaje = '';
  }

  cancelarVinculo(): void {
    this.docenteEnVinculo = null;
    this.carreraParaVincular = null;
  }

  vincular(): void {
    if (!this.docenteEnVinculo || !this.carreraParaVincular) return;
    this.vinculando = true;
    this.docentesApi.asignarCarrera(this.docenteEnVinculo.idDocente, this.carreraParaVincular).subscribe({
      next: () => {
        this.mensaje = 'Vinculado correctamente.';
        this.vinculando = false;
        this.cancelarVinculo();
        this.cargar();
        setTimeout(() => this.mensaje = '', 3000);
      },
      error: (err) => {
        this.error = err?.error?.error || err?.error?.message || 'No se pudo vincular.';
        this.vinculando = false;
        this.cdr.detectChanges();
      }
    });
  }

  quitar(d: DocenteConCarreras, c: { idCarrera: number; nombre: string }): void {
    if (!confirm(`¿Quitar a ${d.nombreCompleto} de ${c.nombre}?`)) return;
    const clave = d.idDocente + '-' + c.idCarrera;
    this.quitando = clave;
    this.docentesApi.quitarCarrera(d.idDocente, c.idCarrera).subscribe({
      next: () => {
        this.mensaje = 'Vínculo eliminado.';
        this.quitando = null;
        this.cargar();
        setTimeout(() => this.mensaje = '', 3000);
      },
      error: (err) => {
        this.error = err?.error?.error || err?.error?.message || 'No se pudo quitar el vínculo.';
        this.quitando = null;
        this.cdr.detectChanges();
      }
    });
  }
}
