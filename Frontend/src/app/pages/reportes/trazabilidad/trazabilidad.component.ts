import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReporteService } from '../service';
import { EstudianteSelect, TrazabilidadEstudiante } from '../model';

@Component({
  selector: 'app-trazabilidad',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './trazabilidad.component.html',
  styleUrls: ['./trazabilidad.component.scss']
})
export class TrazabilidadComponent implements OnInit {
  private svc = inject(ReporteService);
  private cdr = inject(ChangeDetectorRef);

  estudiantes: EstudianteSelect[] = [];
  estudianteSeleccionado?: number;
  trazabilidad: TrazabilidadEstudiante | null = null;
  eventoExpandido: number | null = null;
  cargando = false;
  error = '';

  ngOnInit(): void {
    this.svc.getEstudiantes().subscribe(e => { this.estudiantes = e; this.cdr.detectChanges(); });
  }

  nombreEstudiante(e: EstudianteSelect): string {
    return e.usuario.nombres + ' ' + e.usuario.apellidos + ' - ' + e.usuario.cedula;
  }

  buscar(): void {
    if (!this.estudianteSeleccionado) return;
    this.cargando = true;
    this.error = '';
    this.trazabilidad = null;
    this.eventoExpandido = null;
    this.svc.getTrazabilidad(this.estudianteSeleccionado).subscribe({
      next: t => { this.trazabilidad = t; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.error = 'No se pudo cargar la trazabilidad del estudiante.'; this.cargando = false; this.cdr.detectChanges(); }
    });
  }

  toggle(i: number): void {
    this.eventoExpandido = this.eventoExpandido === i ? null : i;
    this.cdr.detectChanges();
  }
}
