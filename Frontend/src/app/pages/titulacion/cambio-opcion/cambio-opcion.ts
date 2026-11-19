import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { getSessionEntityId, getSessionUser } from '../../../services/session';
import { CatalogosBasicosService, Modalidad, TipoTrabajoTitulacion } from '../../../services/catalogos-basicos.service';
import { MatriculaTitulacionService, MatriculaTitulacion } from '../../../services/matricula-titulacion.service';

const NOMBRE_MODALIDAD_COMPLEXIVO = 'Examen Complexivo';
const SEMANA_LIMITE = 4;

@Component({
  selector: 'app-cambio-opcion-titulacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cambio-opcion.html',
  styleUrl: './cambio-opcion.scss',
})
export class CambioOpcionTitulacionComponent implements OnInit {
  idEstudiante!: number;

  cargando = false;
  guardando = false;
  mensajeError = '';
  mensajeOk = '';

  matricula: MatriculaTitulacion | null = null;
  modalidades: Modalidad[] = [];
  subtipos: TipoTrabajoTitulacion[] = [];

  idOpcionTitulacion: number | null = null;
  idSubtipoTrabajo: number | null = null;
  semanaActual: number | null = null;
  motivo = '';
  motivoMedico = '';
  documentoAdjuntoId: number | null = null;

  constructor(
    private catalogos: CatalogosBasicosService,
    private matriculaService: MatriculaTitulacionService,
  ) {}

  ngOnInit(): void {
    const user = getSessionUser();
    this.idEstudiante = getSessionEntityId(user, 'estudiante') ?? 0;

    this.cargando = true;
    this.matriculaService.listarPorEstudiante(this.idEstudiante).subscribe({
      next: (lista) => {
        this.matricula = (lista || []).find((m) => m.nivel === 'TITULACION_I' && m.estado === 'ACTIVA') ?? null;
        this.cargando = false;
      },
      error: () => {
        this.mensajeError = 'No se pudo cargar tu matrícula';
        this.cargando = false;
      },
    });
    this.catalogos.listarModalidades().subscribe({ next: (m) => (this.modalidades = m || []) });
  }

  get puedeCambiar(): boolean {
    return !!this.matricula && this.matricula.estado === 'ACTIVA' && (this.matricula.numeroCambiosOpcion ?? 0) < 1;
  }

  get fueraDePlazo(): boolean {
    return this.semanaActual != null && this.semanaActual > SEMANA_LIMITE;
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

  cambiar(): void {
    this.mensajeError = '';
    this.mensajeOk = '';

    if (!this.matricula) return;
    if (!this.idOpcionTitulacion) {
      this.mensajeError = 'Debes seleccionar la nueva opción de titulación';
      return;
    }
    if (this.esTic && !this.idSubtipoTrabajo) {
      this.mensajeError = 'Debes seleccionar el subtipo de trabajo';
      return;
    }
    if (this.semanaActual == null) {
      this.mensajeError = 'Debes indicar la semana académica actual';
      return;
    }
    if (this.fueraDePlazo && (!this.motivoMedico.trim() || !this.documentoAdjuntoId)) {
      this.mensajeError = 'Fuera de la semana 4 solo se permite el cambio con motivo médico y su documento de soporte';
      return;
    }

    this.guardando = true;
    this.matriculaService
      .cambiarOpcion(this.matricula.id!, {
        idOpcionTitulacion: this.idOpcionTitulacion,
        idSubtipoTrabajo: this.esTic ? (this.idSubtipoTrabajo ?? undefined) : undefined,
        motivo: this.motivo || undefined,
        semanaActual: this.semanaActual,
        motivoMedico: this.fueraDePlazo ? this.motivoMedico : undefined,
        documentoAdjuntoId: this.fueraDePlazo ? (this.documentoAdjuntoId ?? undefined) : undefined,
      })
      .subscribe({
        next: (actualizada) => {
          this.matricula = actualizada;
          this.guardando = false;
          this.mensajeOk = 'Opción de titulación actualizada correctamente';
        },
        error: (err) => {
          this.guardando = false;
          this.mensajeError = err?.error?.message || 'No se pudo cambiar la opción de titulación';
        },
      });
  }
}
