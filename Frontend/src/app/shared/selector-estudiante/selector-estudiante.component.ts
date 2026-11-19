import { Component, EventEmitter, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EstudianteSelectorService, EstudiantePickerItem } from './estudiante-selector.service';

/**
 * Selector de estudiante reutilizable: un botón abre esta ventana con
 * búsqueda por nombre/cédula y filtros por carrera y curso (semestre).
 * Pensado para reemplazar los <select> gigantes y los campos de "escribe
 * el ID a mano" repetidos por varias páginas (Reportes → Expediente,
 * Actas y Constancias, y las que sigan).
 *
 * Uso:
 *   <app-selector-estudiante #sel (seleccionar)="onElegido($event)" />
 *   <button (click)="sel.abrir()">Seleccionar estudiante</button>
 */
@Component({
  selector: 'app-selector-estudiante',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './selector-estudiante.component.html',
  styleUrl: './selector-estudiante.component.scss',
})
export class SelectorEstudianteComponent {
  @Output() seleccionar = new EventEmitter<EstudiantePickerItem>();

  readonly abierto = signal(false);
  readonly cargando = signal(false);
  readonly error = signal<string | null>(null);
  readonly estudiantes = signal<EstudiantePickerItem[]>([]);

  texto = '';
  carreraFiltro = '';
  cursoFiltro: number | null = null;

  private cargados = false;

  constructor(private readonly svc: EstudianteSelectorService) {}

  abrir(): void {
    this.abierto.set(true);
    this.texto = '';
    this.carreraFiltro = '';
    this.cursoFiltro = null;
    if (!this.cargados) this.cargar();
  }

  cerrar(): void {
    this.abierto.set(false);
  }

  elegir(e: EstudiantePickerItem): void {
    this.seleccionar.emit(e);
    this.cerrar();
  }

  limpiarFiltros(): void {
    this.texto = '';
    this.carreraFiltro = '';
    this.cursoFiltro = null;
  }

  reintentar(): void {
    this.cargados = false;
    this.cargar();
  }

  get carreras(): string[] {
    const nombres = this.estudiantes()
      .map(e => e.carrera)
      .filter((c): c is string => !!c);
    return Array.from(new Set(nombres)).sort((a, b) => a.localeCompare(b));
  }

  get cursos(): number[] {
    const niveles = this.estudiantes()
      .map(e => e.nivelSemestre)
      .filter((n): n is number => n != null);
    return Array.from(new Set(niveles)).sort((a, b) => a - b);
  }

  get resultados(): EstudiantePickerItem[] {
    const q = this.texto.trim().toLowerCase();
    return this.estudiantes().filter(e => {
      if (this.carreraFiltro && e.carrera !== this.carreraFiltro) return false;
      if (this.cursoFiltro != null && e.nivelSemestre !== this.cursoFiltro) return false;
      if (!q) return true;
      return e.nombreCompleto.toLowerCase().includes(q) || e.cedula.toLowerCase().includes(q);
    });
  }

  get hayFiltrosActivos(): boolean {
    return !!this.texto || !!this.carreraFiltro || this.cursoFiltro !== null;
  }

  private cargar(): void {
    this.cargando.set(true);
    this.error.set(null);
    this.svc.listar().subscribe({
      next: data => {
        this.estudiantes.set(data);
        this.cargando.set(false);
        this.cargados = true;
      },
      error: () => {
        this.error.set('No se pudo cargar la lista de estudiantes.');
        this.cargando.set(false);
      },
    });
  }
}
