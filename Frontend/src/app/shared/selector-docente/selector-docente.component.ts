import { Component, EventEmitter, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DocentesService, DocenteSimple } from '../../services/docentes.service';

/**
 * Selector de docente reutilizable — mismo principio que
 * SelectorEstudianteComponent (shared/selector-estudiante): un botón abre
 * una ventana chica con búsqueda por nombre/cédula en vez de un <select>
 * gigante con todos los docentes de la universidad.
 *
 * Uso:
 *   <app-selector-docente #sel (seleccionar)="onElegido($event)" />
 *   <button (click)="sel.abrir()">Seleccionar docente</button>
 */
@Component({
  selector: 'app-selector-docente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './selector-docente.component.html',
  styleUrl: './selector-docente.component.scss',
})
export class SelectorDocenteComponent {
  @Output() seleccionar = new EventEmitter<DocenteSimple>();

  readonly abierto = signal(false);
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  readonly docentes = signal<DocenteSimple[]>([]);

  texto = '';

  private cargados = false;

  constructor(private readonly svc: DocentesService) {}

  abrir(): void {
    this.abierto.set(true);
    this.texto = '';
    if (!this.cargados) this.cargar();
  }

  cerrar(): void {
    this.abierto.set(false);
  }

  elegir(d: DocenteSimple): void {
    this.seleccionar.emit(d);
    this.cerrar();
  }

  reintentar(): void {
    this.cargados = false;
    this.cargar();
  }

  get resultados(): DocenteSimple[] {
    const q = this.texto.trim().toLowerCase();
    if (!q) return this.docentes();
    return this.docentes().filter(d =>
      d.nombreCompleto.toLowerCase().includes(q) || (d.cedula ?? '').toLowerCase().includes(q)
    );
  }

  private cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.svc.listar().subscribe({
      next: data => {
        this.docentes.set(data);
        this.cargando.set(false);
        this.cargados = true;
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de docentes.');
        this.cargando.set(false);
      },
    });
  }
}
