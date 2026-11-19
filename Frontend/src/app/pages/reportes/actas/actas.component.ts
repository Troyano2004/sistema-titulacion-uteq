import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ReporteService } from '../service';
import { SelectorEstudianteComponent } from '../../../shared/selector-estudiante/selector-estudiante.component';
import { EstudiantePickerItem } from '../../../shared/selector-estudiante/estudiante-selector.service';

interface ProyectoDeEstudiante {
  idProyecto: number;
  titulo: string;
  estado: string;
}

@Component({
  selector: 'app-reporte-actas',
  standalone: true,
  imports: [CommonModule, FormsModule, SelectorEstudianteComponent],
  templateUrl: './actas.component.html',
  styleUrls: ['./actas.component.scss']
})
export class ActasComponent {
  private readonly proyectoPorEstudianteUrl = 'http://localhost:8080/api/proyectos-titulacion/estudiante';

  estudianteElegido: EstudiantePickerItem | null = null;
  proyectoIdActas?: number;
  proyectoTitulo?: string;
  buscandoProyecto = false;
  errorProyecto: string | null = null;

  constructor(
    private readonly svc: ReporteService,
    private readonly http: HttpClient,
  ) {}

  onEstudianteSeleccionado(e: EstudiantePickerItem): void {
    this.estudianteElegido = e;
    this.proyectoIdActas = undefined;
    this.proyectoTitulo = undefined;
    this.errorProyecto = null;
    this.buscandoProyecto = true;

    this.http.get<ProyectoDeEstudiante>(`${this.proyectoPorEstudianteUrl}/${e.idEstudiante}`, { withCredentials: true })
      .subscribe({
        next: (p) => {
          this.proyectoIdActas = p.idProyecto;
          this.proyectoTitulo = p.titulo;
          this.buscandoProyecto = false;
        },
        error: (err) => {
          this.buscandoProyecto = false;
          this.errorProyecto = err?.status === 404
            ? 'Este estudiante no tiene un proyecto de titulación registrado.'
            : 'No se pudo obtener el proyecto de este estudiante.';
        }
      });
  }

  actasTutoria()      { if (this.proyectoIdActas) this.svc.actasTutoriaPdf(this.proyectoIdActas); }
  actasSustentacion() { if (this.proyectoIdActas) this.svc.actasSustentacionPdf(this.proyectoIdActas); }
}
