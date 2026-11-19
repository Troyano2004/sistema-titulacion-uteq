import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReporteService } from '../service';
import { SelectorEstudianteComponent } from '../../../shared/selector-estudiante/selector-estudiante.component';
import { EstudiantePickerItem } from '../../../shared/selector-estudiante/estudiante-selector.service';

@Component({
  selector: 'app-reporte-expediente',
  standalone: true,
  imports: [CommonModule, FormsModule, SelectorEstudianteComponent],
  templateUrl: './expediente.component.html',
  styleUrls: ['./expediente.component.scss']
})
export class ExpedienteComponent {
  estudianteElegido: EstudiantePickerItem | null = null;

  constructor(private readonly svc: ReporteService) {}

  get estudianteSeleccionado(): number | undefined {
    return this.estudianteElegido?.idEstudiante;
  }

  onEstudianteSeleccionado(e: EstudiantePickerItem): void {
    this.estudianteElegido = e;
  }

  expedientePdf()   { if (this.estudianteSeleccionado) this.svc.expedientePdf(this.estudianteSeleccionado); }
  expedienteExcel() { if (this.estudianteSeleccionado) this.svc.expedienteExcel(this.estudianteSeleccionado); }
}
