import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  CalendarioTitulacionService,
  EventoCalendario,
  PeriodoAval
} from '../../../services/calendario-titulacion.service';
import { CatalogosBasicosService, Facultad, PeriodoTitulacion } from '../../../services/catalogos-basicos.service';
import { hasAnyRole } from '../../../services/session';

const COLOR_POR_TIPO: Record<string, string> = {
  EXAMEN_COMPLEXIVO_FINAL: '#3182ce',
  EXAMEN_COMPLEXIVO_SUPLETORIO: '#805ad5',
  PREDEFENSA: '#d69e2e',
  SUSTENTACION: '#38a169',
  PLAZO_PROPUESTA_TIC: '#e53e3e',
  PLAZO_CIERRE_CORTE1: '#dd6b20',
  PLAZO_CIERRE_CORTE2: '#dd6b20',
};

@Component({
  selector: 'app-calendario-titulacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './calendario-titulacion.html',
  styleUrl: './calendario-titulacion.scss'
})
export class CalendarioTitulacionComponent implements OnInit {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);

  periodos = signal<PeriodoTitulacion[]>([]);
  facultades = signal<Facultad[]>([]);
  eventos = signal<EventoCalendario[]>([]);
  aval = signal<PeriodoAval | null>(null);
  eventoSeleccionado = signal<EventoCalendario | null>(null);

  idPeriodoSeleccionado: number | null = null;
  idFacultadSeleccionada: number | null = null;
  filtroTipo = '';

  puedeGestionarAval = hasAnyRole('DECANO', 'COORDINADOR', 'ADMIN', 'DIRECCION_ACADEMICA');

  constructor(
    private calendarioApi: CalendarioTitulacionService,
    private catalogosApi: CatalogosBasicosService
  ) {}

  ngOnInit(): void {
    this.catalogosApi.listarPeriodos().subscribe({
      next: data => {
        this.periodos.set(data);
        const activo = data.find(p => p.activo);
        if (activo) {
          this.idPeriodoSeleccionado = activo.idPeriodo ?? null;
          this.onFiltroChange();
        }
      },
      error: () => this.error.set('No se pudieron cargar los periodos')
    });
    this.catalogosApi.listarFacultades().subscribe({
      next: data => this.facultades.set(data),
      error: () => this.error.set('No se pudieron cargar las facultades')
    });
  }

  get eventosFiltrados(): EventoCalendario[] {
    const eventos = this.eventos();
    return this.filtroTipo ? eventos.filter(e => e.tipoEvento === this.filtroTipo) : eventos;
  }

  get tiposDisponibles(): string[] {
    return Array.from(new Set(this.eventos().map(e => e.tipoEvento))).sort();
  }

  colorDeTipo(tipo: string): string {
    return COLOR_POR_TIPO[tipo] ?? '#718096';
  }

  onFiltroChange(): void {
    this.error.set(null);
    this.ok.set(null);
    const periodoDesc = this.periodos().find(p => p.idPeriodo === this.idPeriodoSeleccionado)?.descripcion;
    this.loading.set(true);
    this.calendarioApi.obtenerEventos(periodoDesc, this.idFacultadSeleccionada ?? undefined)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: data => this.eventos.set(data),
        error: () => this.error.set('No se pudieron cargar los eventos del calendario')
      });

    if (this.idPeriodoSeleccionado) {
      this.calendarioApi.obtenerAval(this.idPeriodoSeleccionado).subscribe({
        next: data => this.aval.set(data),
        error: () => this.aval.set(null)
      });
    }
  }

  verDetalle(evento: EventoCalendario): void {
    this.eventoSeleccionado.set(evento);
  }

  cerrarDetalle(): void {
    this.eventoSeleccionado.set(null);
  }

  darAval(): void {
    if (!this.idPeriodoSeleccionado) return;
    const avaladoPor = prompt('Nombre de quien otorga el aval de Unidad de Registro:');
    if (!avaladoPor || !avaladoPor.trim()) return;

    this.loading.set(true);
    this.calendarioApi.darAval(this.idPeriodoSeleccionado, avaladoPor.trim())
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: data => {
          this.aval.set(data);
          this.ok.set('Aval de Unidad de Registro registrado correctamente');
        },
        error: (err: any) => this.error.set(err?.error?.message ?? 'Error al registrar el aval')
      });
  }
}
