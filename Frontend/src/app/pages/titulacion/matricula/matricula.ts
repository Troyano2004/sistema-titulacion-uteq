import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { getSessionEntityId, getSessionUser } from '../../../services/session';
import { CatalogosBasicosService, Modalidad, TipoTrabajoTitulacion, PeriodoTitulacion } from '../../../services/catalogos-basicos.service';
import { MatriculaTitulacionService, MatriculaTitulacionRequest, NivelMatricula } from '../../../services/matricula-titulacion.service';

const NOMBRE_MODALIDAD_COMPLEXIVO = 'Examen Complexivo';

@Component({
  selector: 'app-matricula-titulacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './matricula.html',
  styleUrl: './matricula.scss',
})
export class MatriculaTitulacionComponent implements OnInit {
  idEstudiante!: number;

  cargando = false;
  guardando = false;
  mensajeError = '';
  mensajeOk = '';

  modalidades: Modalidad[] = [];
  subtipos: TipoTrabajoTitulacion[] = [];
  periodos: PeriodoTitulacion[] = [];

  nivel: NivelMatricula = 'TITULACION_I';
  idOpcionTitulacion: number | null = null;
  idSubtipoTrabajo: number | null = null;
  periodoAcademico = '';

  // Art. 18 — solo para TITULACION_II
  idiomaExtranjeroAprobado = false;
  practicasLaboralesAprobadas = false;
  servicioComunitarioAprobado = false;

  constructor(
    private catalogos: CatalogosBasicosService,
    private matriculaService: MatriculaTitulacionService,
  ) {}

  ngOnInit(): void {
    const user = getSessionUser();
    this.idEstudiante = getSessionEntityId(user, 'estudiante') ?? 0;

    this.cargando = true;
    this.catalogos.listarModalidades().subscribe({
      next: (m) => (this.modalidades = m || []),
      error: () => (this.mensajeError = 'No se pudieron cargar las opciones de titulación'),
    });
    this.catalogos.listarPeriodosActivos().subscribe({
      next: (p) => {
        this.periodos = p || [];
        if (this.periodos.length) this.periodoAcademico = this.periodos[0].descripcion;
        this.cargando = false;
      },
      error: () => {
        this.cargando = false;
      },
    });
  }

  get esTic(): boolean {
    const opcion = this.modalidades.find((m) => m.idModalidad === this.idOpcionTitulacion);
    return !!opcion && opcion.nombre !== NOMBRE_MODALIDAD_COMPLEXIVO;
  }

  onCambiarOpcion(): void {
    this.idSubtipoTrabajo = null;
    this.subtipos = [];
    if (this.idOpcionTitulacion && this.esTic) {
      this.catalogos.listarTiposTrabajoPorModalidad(this.idOpcionTitulacion).subscribe({
        next: (s) => (this.subtipos = s || []),
      });
    }
  }

  matricularse(): void {
    this.mensajeError = '';
    this.mensajeOk = '';

    if (!this.idEstudiante) {
      this.mensajeError = 'No se pudo identificar al estudiante en la sesión';
      return;
    }
    if (!this.idOpcionTitulacion) {
      this.mensajeError = 'Debes seleccionar una opción de titulación';
      return;
    }
    if (this.esTic && !this.idSubtipoTrabajo) {
      this.mensajeError = 'Debes seleccionar el subtipo de trabajo de integración curricular';
      return;
    }

    const req: MatriculaTitulacionRequest = {
      idEstudiante: this.idEstudiante,
      periodoAcademico: this.periodoAcademico,
      nivel: this.nivel,
      idOpcionTitulacion: this.idOpcionTitulacion,
      idSubtipoTrabajo: this.esTic ? (this.idSubtipoTrabajo ?? undefined) : undefined,
    };

    if (this.nivel === 'TITULACION_II') {
      req.idiomaExtranjeroAprobado = this.idiomaExtranjeroAprobado;
      req.practicasLaboralesAprobadas = this.practicasLaboralesAprobadas;
      req.servicioComunitarioAprobado = this.servicioComunitarioAprobado;
    }

    this.guardando = true;
    this.matriculaService.crear(req).subscribe({
      next: () => {
        this.guardando = false;
        this.mensajeOk = 'Matrícula registrada correctamente';
      },
      error: (err) => {
        this.guardando = false;
        this.mensajeError = err?.error?.message || 'No se pudo registrar la matrícula';
      },
    });
  }
}
