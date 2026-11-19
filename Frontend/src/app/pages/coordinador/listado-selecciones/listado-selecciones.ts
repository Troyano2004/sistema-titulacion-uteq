import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogosBasicosService, Carrera } from '../../../services/catalogos-basicos.service';
import { TutoriaListadoService, ListadoSeleccionesResponse } from '../../../services/tutoria-listado.service';

@Component({
  selector: 'app-listado-selecciones',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './listado-selecciones.html',
  styleUrl: './listado-selecciones.scss',
})
export class ListadoSeleccionesComponent implements OnInit {
  carreras: Carrera[] = [];
  idCarrera: number | null = null;
  periodo = '';

  cargando = false;
  mensajeError = '';
  resultado: ListadoSeleccionesResponse | null = null;

  constructor(
    private catalogos: CatalogosBasicosService,
    private tutoriaListadoService: TutoriaListadoService,
  ) {}

  ngOnInit(): void {
    this.catalogos.listarCarreras().subscribe({ next: (c) => (this.carreras = c || []) });
  }

  get totalEstudiantes(): number {
    if (!this.resultado) return 0;
    return this.resultado.grupos.reduce((acc, g) => acc + g.estudiantes.length, 0);
  }

  buscar(): void {
    this.mensajeError = '';
    this.resultado = null;
    if (!this.idCarrera) {
      this.mensajeError = 'Selecciona una carrera';
      return;
    }

    this.cargando = true;
    this.tutoriaListadoService.listadoSelecciones(this.idCarrera, this.periodo || undefined).subscribe({
      next: (r) => {
        this.resultado = r;
        this.cargando = false;
      },
      error: (err) => {
        this.cargando = false;
        this.mensajeError = err?.error?.message || 'No se pudo cargar el listado';
      },
    });
  }

  exportarCsv(): void {
    if (!this.resultado) return;

    const filas = [['Opción de titulación', 'Cédula', 'Estudiante']];
    for (const grupo of this.resultado.grupos) {
      for (const e of grupo.estudiantes) {
        filas.push([grupo.opcionTitulacion, e.cedula || '', e.nombreCompleto || '']);
      }
    }

    const csv = filas.map((fila) => fila.map((c) => `"${(c || '').replace(/"/g, '""')}"`).join(',')).join('\n');
    const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `listado-selecciones-${this.resultado.nombreCarrera}-${this.resultado.periodoAcademico || 'sin-periodo'}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
