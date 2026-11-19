import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators, FormArray } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  BancoPreguntasService,
  PreguntaComplexivoResponse,
  EstadisticasBancoDto
} from '../../../services/banco-preguntas.service';
import { EjeTematicoService, EjeTematico } from '../../../services/eje-tematico.service';
import { CatalogosBasicosService, Carrera } from '../../../services/catalogos-basicos.service';
import { getSessionUser, getSessionEntityId } from '../../../services/session';

@Component({
  selector: 'app-banco-preguntas',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './banco-preguntas.html',
  styleUrl: './banco-preguntas.scss'
})
export class BancoPreguntasComponent implements OnInit {
  loading = signal(false);
  error = signal<string | null>(null);
  ok = signal<string | null>(null);

  carreras = signal<Carrera[]>([]);
  ejes = signal<EjeTematico[]>([]);
  preguntas = signal<PreguntaComplexivoResponse[]>([]);
  estadisticas = signal<EstadisticasBancoDto | null>(null);

  idCarreraSeleccionada: number | null = null;
  filtroEje: number | null = null;
  filtroEstado = '';

  mostrarFormulario = signal(false);
  editandoId: number | null = null;
  previsualizando = signal(false);

  form: FormGroup;

  private idDocente = 0;

  constructor(
    private bancoApi: BancoPreguntasService,
    private ejeApi: EjeTematicoService,
    private catalogosApi: CatalogosBasicosService,
    private fb: FormBuilder
  ) {
    this.form = this.fb.group({
      idEjeTematico: [null, Validators.required],
      enunciado: ['', Validators.required],
      dificultad: ['BASICA', Validators.required],
      periodoAcademico: ['', Validators.required],
      estado: ['EN_REVISION', Validators.required],
      opciones: this.fb.array([
        this.opcionGroup(),
        this.opcionGroup()
      ])
    });
  }

  ngOnInit(): void {
    const user = getSessionUser();
    this.idDocente = getSessionEntityId(user, 'docente') ?? 0;
    this.catalogosApi.listarCarreras().subscribe({
      next: data => this.carreras.set(data),
      error: () => this.error.set('No se pudieron cargar las carreras')
    });
  }

  get opciones(): FormArray {
    return this.form.get('opciones') as FormArray;
  }

  private opcionGroup(esCorrecta = false) {
    return this.fb.group({
      id: [null],
      texto: ['', Validators.required],
      esCorrecta: [esCorrecta]
    });
  }

  agregarOpcion(): void {
    if (this.opciones.length >= 5) return;
    this.opciones.push(this.opcionGroup());
  }

  quitarOpcion(i: number): void {
    if (this.opciones.length <= 2) return;
    this.opciones.removeAt(i);
  }

  marcarCorrecta(i: number): void {
    this.opciones.controls.forEach((c, idx) => c.get('esCorrecta')!.setValue(idx === i));
  }

  onCarreraChange(): void {
    this.ok.set(null);
    this.error.set(null);
    this.filtroEje = null;
    if (!this.idCarreraSeleccionada) {
      this.ejes.set([]);
      this.preguntas.set([]);
      this.estadisticas.set(null);
      return;
    }
    this.ejeApi.listarPorCarrera(this.idCarreraSeleccionada).subscribe({
      next: data => this.ejes.set(data),
      error: () => this.error.set('No se pudieron cargar los ejes temáticos de la carrera')
    });
    this.cargarPreguntas();
    this.cargarEstadisticas();
  }

  aplicarFiltros(): void {
    this.cargarPreguntas();
  }

  nuevaPregunta(): void {
    this.editandoId = null;
    this.form.reset({ dificultad: 'BASICA', estado: 'EN_REVISION', periodoAcademico: '' });
    this.opciones.clear();
    this.opciones.push(this.opcionGroup(true));
    this.opciones.push(this.opcionGroup(false));
    this.mostrarFormulario.set(true);
    this.previsualizando.set(false);
  }

  editar(p: PreguntaComplexivoResponse): void {
    this.editandoId = p.id;
    this.form.reset({
      idEjeTematico: p.idEjeTematico,
      enunciado: p.enunciado,
      dificultad: p.dificultad,
      periodoAcademico: p.periodoAcademico,
      estado: p.estado
    });
    this.opciones.clear();
    p.opciones.forEach(o => this.opciones.push(this.fb.group({
      id: [o.id], texto: [o.texto, Validators.required], esCorrecta: [o.esCorrecta]
    })));
    this.mostrarFormulario.set(true);
    this.previsualizando.set(false);
  }

  cancelar(): void {
    this.mostrarFormulario.set(false);
    this.editandoId = null;
  }

  togglePreview(): void {
    this.previsualizando.set(!this.previsualizando());
  }

  guardar(): void {
    if (this.form.invalid || !this.idCarreraSeleccionada) {
      this.form.markAllAsTouched();
      return;
    }
    const correctas = this.opciones.value.filter((o: any) => o.esCorrecta).length;
    if (correctas !== 1) {
      this.error.set('Debe marcar exactamente 1 opción como correcta');
      return;
    }

    const v = this.form.value;
    const req = {
      idEjeTematico: v.idEjeTematico,
      idCarrera: this.idCarreraSeleccionada,
      enunciado: v.enunciado,
      dificultad: v.dificultad,
      estado: v.estado,
      periodoAcademico: v.periodoAcademico,
      idDocenteCreador: this.idDocente,
      opciones: v.opciones
    };

    this.loading.set(true);
    this.error.set(null);
    this.ok.set(null);
    const obs = this.editandoId
      ? this.bancoApi.actualizar(this.editandoId, req)
      : this.bancoApi.crear(req);

    obs.pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => {
        this.ok.set(this.editandoId ? 'Pregunta actualizada' : 'Pregunta creada');
        this.mostrarFormulario.set(false);
        this.cargarPreguntas();
        this.cargarEstadisticas();
      },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al guardar la pregunta')
    });
  }

  desactivar(p: PreguntaComplexivoResponse): void {
    if (!confirm(`¿Desactivar la pregunta "${p.enunciado.substring(0, 40)}..."?`)) return;
    this.bancoApi.desactivar(p.id).subscribe({
      next: () => {
        this.ok.set('Pregunta desactivada');
        this.cargarPreguntas();
        this.cargarEstadisticas();
      },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al desactivar')
    });
  }

  private cargarPreguntas(): void {
    if (!this.idCarreraSeleccionada) return;
    this.loading.set(true);
    this.bancoApi.listar(this.idCarreraSeleccionada, this.filtroEje ?? undefined, this.filtroEstado || undefined)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: data => this.preguntas.set(data),
        error: () => this.error.set('Error al cargar las preguntas')
      });
  }

  private cargarEstadisticas(): void {
    if (!this.idCarreraSeleccionada) return;
    this.bancoApi.estadisticas(this.idCarreraSeleccionada).subscribe({
      next: data => this.estadisticas.set(data),
      error: () => {}
    });
  }

  objectKeys(obj: Record<string, number> | null | undefined): string[] {
    return obj ? Object.keys(obj) : [];
  }
}
