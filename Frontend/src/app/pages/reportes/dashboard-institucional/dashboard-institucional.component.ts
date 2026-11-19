import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReporteService } from '../service';
import { CatalogosBasicosService, Facultad, Carrera } from '../../../services/catalogos-basicos.service';
import { PeriodoSelect, ResultadosFacultad, DashboardReportes, ReporteExpedientes } from '../model';

@Component({
  selector: 'app-dashboard-institucional',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard-institucional.component.html',
  styleUrls: ['./dashboard-institucional.component.scss']
})
export class DashboardInstitucionalComponent implements OnInit {
  private svc = inject(ReporteService);
  private catalogosSvc = inject(CatalogosBasicosService);
  private cdr = inject(ChangeDetectorRef);

  periodos: PeriodoSelect[] = [];
  facultades: Facultad[] = [];
  carreras: Carrera[] = [];

  periodoSeleccionado = '';
  facultadSeleccionada?: number;
  carreraSeleccionada?: number;

  dashboard: DashboardReportes | null = null;
  resultadosFacultad: ResultadosFacultad | null = null;
  reporteExpedientes: ReporteExpedientes | null = null;

  cargando = false;
  error = '';

  ngOnInit(): void {
    this.svc.getPeriodos().subscribe(p => {
      this.periodos = p;
      const activo = p.find(x => x.activo);
      this.periodoSeleccionado = activo ? activo.descripcion : (p[0]?.descripcion ?? '');
      if (this.periodoSeleccionado) this.aplicarFiltros();
      this.cdr.detectChanges();
    });
    this.catalogosSvc.listarFacultades().subscribe(f => { this.facultades = f; this.cdr.detectChanges(); });
    this.catalogosSvc.listarCarreras().subscribe(c => { this.carreras = c; this.cdr.detectChanges(); });
  }

  get carrerasFiltradas(): Carrera[] {
    if (!this.facultadSeleccionada) return this.carreras;
    return this.carreras.filter(c => c.idFacultad === this.facultadSeleccionada);
  }

  aplicarFiltros(): void {
    if (!this.periodoSeleccionado) return;
    this.cargando = true;
    this.error = '';
    this.resultadosFacultad = null;

    this.svc.getDashboard(this.periodoSeleccionado).subscribe({
      next: d => { this.dashboard = d; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.error = 'No se pudo cargar el dashboard general.'; this.cargando = false; this.cdr.detectChanges(); }
    });

    if (this.facultadSeleccionada) {
      this.svc.getResultadosFacultad(this.facultadSeleccionada, this.periodoSeleccionado).subscribe({
        next: r => { this.resultadosFacultad = r; this.cdr.detectChanges(); },
        error: () => { this.error = 'No se pudo cargar el reporte de la facultad seleccionada.'; this.cdr.detectChanges(); }
      });
    }

    this.svc.getReporteExpedientes(this.carreraSeleccionada, this.periodoSeleccionado).subscribe({
      next: e => { this.reporteExpedientes = e; this.cdr.detectChanges(); },
      error: () => { /* sección opcional, no bloquea el resto del dashboard */ }
    });
  }

  exportarPdf(): void {
    if (!this.facultadSeleccionada || !this.periodoSeleccionado) return;
    this.svc.resultadosFacultadPdf(this.facultadSeleccionada, this.periodoSeleccionado);
  }

  exportarCsv(): void {
    if (!this.dashboard) return;
    const filas: string[] = ['Sección;Etiqueta;Total;Aprobados;Reprobados'];
    for (const m of this.dashboard.porModalidad) {
      filas.push(`Modalidad;${m.modalidad};${m.total};${m.aprobados};${m.reprobados}`);
    }
    if (this.resultadosFacultad) {
      for (const c of this.resultadosFacultad.carreras) {
        filas.push(`Carrera;${c.carrera};${c.totalEstudiantes};;${c.tasaAprobacion.toFixed(1)}%`);
      }
    }
    const csv = '﻿' + filas.join('\r\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `Dashboard_Reportes_${this.periodoSeleccionado}.csv`;
    a.click();
    URL.revokeObjectURL(a.href);
  }

  barWidth(valor: number, total: number): string {
    if (!total) return '0%';
    return Math.min(100, (valor * 100 / total)) + '%';
  }

  nombreFacultad(id?: number): string {
    return this.facultades.find(f => f.idFacultad === id)?.nombre ?? '';
  }
}
