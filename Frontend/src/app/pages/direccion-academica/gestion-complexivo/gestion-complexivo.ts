import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  ExamenComplexivoService,
  ExamenAplicacionResponse,
  DelegadoDisponibleDto
} from '../../../services/examen-complexivo.service';

@Component({
  selector: 'app-gestion-complexivo',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './gestion-complexivo.html',
  styleUrl: './gestion-complexivo.scss'
})
export class GestionComplexivoComponent implements OnInit {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);

  tab = signal<'programar' | 'delegados' | 'resultados' | 'fraude'>('programar');

  formProgramar: FormGroup;
  formFraude: FormGroup;

  idEstudianteConsulta: number | null = null;
  examenesEstudiante = signal<ExamenAplicacionResponse[]>([]);

  idExamenDelegado: number | null = null;
  idCarreraEstudianteDelegado: number | null = null;
  delegados = signal<DelegadoDisponibleDto[]>([]);
  idDocenteSeleccionado: number | null = null;

  idExamenFraude: number | null = null;

  constructor(private examenApi: ExamenComplexivoService, private fb: FormBuilder) {
    this.formProgramar = this.fb.group({
      idEstudiante: [null, Validators.required],
      tipo: ['FINAL', Validators.required],
      fechaProgramada: ['', Validators.required]
    });
    this.formFraude = this.fb.group({
      descripcionIncidente: ['', Validators.required],
      evidencia: ['']
    });
  }

  ngOnInit(): void {}

  setTab(t: 'programar' | 'delegados' | 'resultados' | 'fraude'): void {
    this.tab.set(t);
    this.error.set(null);
    this.ok.set(null);
  }

  programar(): void {
    if (this.formProgramar.invalid) {
      this.formProgramar.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);
    this.examenApi.programar(this.formProgramar.value)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: resp => {
          this.ok.set(`Examen programado con id ${resp.id} para el ${resp.fechaProgramada}.`);
          this.formProgramar.reset({ tipo: 'FINAL' });
        },
        error: (err: any) => this.error.set(err?.error?.message ?? 'Error al programar el examen')
      });
  }

  buscarExamenesEstudiante(): void {
    if (!this.idEstudianteConsulta) return;
    this.loading.set(true);
    this.examenApi.listarPorEstudiante(this.idEstudianteConsulta)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: data => this.examenesEstudiante.set(data),
        error: () => this.error.set('No se pudieron cargar los exámenes del estudiante')
      });
  }

  buscarDelegadosDisponibles(): void {
    if (!this.idCarreraEstudianteDelegado) return;
    this.examenApi.delegadosDisponibles(this.idCarreraEstudianteDelegado).subscribe({
      next: data => this.delegados.set(data),
      error: () => this.error.set('No se pudieron cargar los delegados disponibles')
    });
  }

  asignarDelegado(): void {
    if (!this.idExamenDelegado || !this.idDocenteSeleccionado) {
      this.error.set('Selecciona el examen y el docente delegado');
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);
    this.examenApi.asignarDelegado(this.idExamenDelegado, this.idDocenteSeleccionado)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: resp => this.ok.set(`Delegado asignado: ${resp.nombreDelegado}`),
        error: (err: any) => this.error.set(err?.error?.message ?? 'Error al asignar el delegado')
      });
  }

  registrarFraude(): void {
    if (!this.idExamenFraude || this.formFraude.invalid) {
      this.formFraude.markAllAsTouched();
      return;
    }
    if (!confirm('¿Confirmas anular este examen por fraude académico? Esta acción califica el examen con 0 y no se puede deshacer.')) return;

    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);
    this.examenApi.registrarFraude(this.idExamenFraude, this.formFraude.value)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => {
          this.ok.set('Examen anulado por fraude académico. Incidente registrado en auditoría.');
          this.formFraude.reset();
        },
        error: (err: any) => this.error.set(err?.error?.message ?? 'Error al registrar el fraude')
      });
  }
}
