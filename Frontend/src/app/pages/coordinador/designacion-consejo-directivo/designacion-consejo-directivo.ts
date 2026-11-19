import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { getSessionUser } from '../../../services/session';
import { CatalogosBasicosService, Carrera } from '../../../services/catalogos-basicos.service';
import {
  PropuestaResolucionService,
  PropuestaPendiente,
  DocenteElegible,
} from '../../../services/propuesta-resolucion.service';

@Component({
  selector: 'app-designacion-consejo-directivo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './designacion-consejo-directivo.html',
  styleUrl: './designacion-consejo-directivo.scss',
})
export class DesignacionConsejoDirectivoComponent implements OnInit {
  idUsuario = Number(getSessionUser()?.['idUsuario']) || null;

  carreras: Carrera[] = [];
  idCarreraFiltro: number | null = null;

  cargando = false;
  mensajeError = '';
  pendientes: PropuestaPendiente[] = [];

  propuestaSeleccionada: PropuestaPendiente | null = null;
  docentesElegibles: DocenteElegible[] = [];
  cargandoDocentes = false;

  directorId: number | null = null;
  presidenteTribunalId: number | null = null;
  vocal1Id: number | null = null;
  vocal2Id: number | null = null;
  observaciones = '';

  designando = false;
  errorDesignacion = '';
  okDesignacion = '';

  constructor(
    private catalogos: CatalogosBasicosService,
    private resolucionService: PropuestaResolucionService,
  ) {}

  ngOnInit(): void {
    this.catalogos.listarCarreras().subscribe({ next: (c) => (this.carreras = c || []) });
    this.buscar();
  }

  buscar(): void {
    this.cargando = true;
    this.mensajeError = '';
    this.resolucionService.listarPendientes(this.idCarreraFiltro ?? undefined).subscribe({
      next: (p) => {
        this.pendientes = p || [];
        this.cargando = false;
      },
      error: (err) => {
        this.cargando = false;
        this.mensajeError = err?.error?.message || 'No se pudo cargar el listado de propuestas pendientes';
      },
    });
  }

  abrirDesignacion(p: PropuestaPendiente): void {
    this.propuestaSeleccionada = p;
    this.directorId = null;
    this.presidenteTribunalId = null;
    this.vocal1Id = null;
    this.vocal2Id = null;
    this.observaciones = '';
    this.errorDesignacion = '';
    this.okDesignacion = '';
    this.docentesElegibles = [];

    this.cargandoDocentes = true;
    this.resolucionService.listarDocentesElegibles(p.idCarrera).subscribe({
      next: (d) => {
        this.docentesElegibles = d || [];
        this.cargandoDocentes = false;
      },
      error: () => {
        this.cargandoDocentes = false;
      },
    });
  }

  cerrarDesignacion(): void {
    this.propuestaSeleccionada = null;
  }

  aprobarYDesignar(): void {
    if (!this.propuestaSeleccionada || !this.idUsuario) return;
    if (!this.directorId || !this.presidenteTribunalId || !this.vocal1Id || !this.vocal2Id) {
      this.errorDesignacion = 'Debes seleccionar Director, Presidente y los 2 Vocales del Tribunal';
      return;
    }

    this.designando = true;
    this.errorDesignacion = '';
    this.resolucionService
      .aprobarYDesignar(this.propuestaSeleccionada.idPropuesta, {
        idUsuario: this.idUsuario,
        directorId: this.directorId,
        presidenteTribunalId: this.presidenteTribunalId,
        vocal1Id: this.vocal1Id,
        vocal2Id: this.vocal2Id,
        observaciones: this.observaciones || undefined,
      })
      .subscribe({
        next: () => {
          this.designando = false;
          this.okDesignacion = 'Director y Tribunal designados correctamente';
          this.buscar();
          setTimeout(() => this.cerrarDesignacion(), 1200);
        },
        error: (err) => {
          this.designando = false;
          this.errorDesignacion = err?.error?.message || 'No se pudo completar la designación';
        },
      });
  }
}
