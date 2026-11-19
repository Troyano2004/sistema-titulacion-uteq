import { Component, OnInit, OnDestroy, signal, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import {
  ExamenComplexivoService,
  ExamenAplicacionResponse,
  PreguntaExamenDto,
  ResultadoExamenDto
} from '../../../services/examen-complexivo.service';
import { getSessionUser, getSessionEntityId } from '../../../services/session';

type Fase = 'cargando' | 'sin-examen' | 'ingreso' | 'listo-para-iniciar' | 'en-curso' | 'resultado' | 'anulado';

@Component({
  selector: 'app-examen-complexivo-estudiante',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './examen-complexivo.html',
  styleUrl: './examen-complexivo.scss'
})
export class ExamenComplexivoEstudianteComponent implements OnInit, OnDestroy {
  @ViewChild('contenedorExamen') contenedorExamen?: ElementRef<HTMLElement>;

  fase = signal<Fase>('cargando');
  error = signal<string | null>(null);

  examen = signal<ExamenAplicacionResponse | null>(null);
  preguntas = signal<PreguntaExamenDto[]>([]);
  respuestas = signal<Record<number, number | null>>({});
  indiceActual = signal(0);
  resultado = signal<ResultadoExamenDto | null>(null);

  documentoIdentidad = '';
  firmadoCompromisosEticos = false;

  tiempoRestanteMs = signal(0);
  horaFinExamen: Date | null = null;

  private idEstudiante = 0;
  private timerId: any = null;
  private finalizando = false;

  constructor(private examenApi: ExamenComplexivoService) {}

  ngOnInit(): void {
    const user = getSessionUser();
    this.idEstudiante = getSessionEntityId(user, 'estudiante') ?? 0;
    this.cargarExamenes();
  }

  ngOnDestroy(): void {
    if (this.timerId) clearInterval(this.timerId);
  }

  get preguntaActual(): PreguntaExamenDto | null {
    return this.preguntas()[this.indiceActual()] ?? null;
  }

  get totalRespondidas(): number {
    const r = this.respuestas();
    return Object.values(r).filter(v => v !== null && v !== undefined).length;
  }

  get totalSinResponder(): number {
    return this.preguntas().length - this.totalRespondidas;
  }

  get minutosRestantes(): number {
    return Math.floor(this.tiempoRestanteMs() / 60000);
  }

  get segundosRestantes(): number {
    return Math.floor((this.tiempoRestanteMs() % 60000) / 1000);
  }

  private cargarExamenes(): void {
    this.fase.set('cargando');
    this.examenApi.listarPorEstudiante(this.idEstudiante).subscribe({
      next: lista => {
        const enCurso = lista.find(e => e.estado === 'EN_CURSO');
        const programado = lista.find(e => e.estado === 'PROGRAMADO');
        const finalizado = lista.filter(e => e.estado === 'FINALIZADO').sort((a, b) => b.id - a.id)[0];
        const anulado = lista.filter(e => e.estado === 'ANULADO').sort((a, b) => b.id - a.id)[0];

        if (enCurso) {
          this.reanudarExamenEnCurso(enCurso);
        } else if (programado) {
          this.examen.set(programado);
          this.fase.set('ingreso');
        } else if (finalizado) {
          this.examen.set(finalizado);
          this.resultado.set({
            idExamen: finalizado.id,
            calificacion: finalizado.calificacion ?? 0,
            totalPreguntasCorrectas: finalizado.totalPreguntasCorrectas ?? 0,
            totalPreguntas: 100,
            aprobado: (finalizado.calificacion ?? 0) >= 7
          });
          this.fase.set('resultado');
        } else if (anulado) {
          this.examen.set(anulado);
          this.fase.set('anulado');
        } else {
          this.fase.set('sin-examen');
        }
      },
      error: () => {
        this.error.set('No se pudo cargar la información del examen');
        this.fase.set('sin-examen');
      }
    });
  }

  registrarIngreso(): void {
    const ex = this.examen();
    if (!ex) return;
    if (!this.documentoIdentidad.trim()) {
      this.error.set('El documento de identidad es obligatorio');
      return;
    }
    this.examenApi.registrarIngreso(ex.id, {
      documentoIdentidad: this.documentoIdentidad,
      firmadoCompromisosEticos: this.firmadoCompromisosEticos
    }).subscribe({
      next: () => {
        if (!this.firmadoCompromisosEticos) {
          this.error.set('Debe firmar los compromisos éticos para poder iniciar el examen');
          return;
        }
        this.error.set(null);
        this.fase.set('listo-para-iniciar');
      },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al registrar el ingreso')
    });
  }

  iniciarExamen(): void {
    const ex = this.examen();
    if (!ex) return;
    if (!confirm('¿Estás seguro de iniciar el examen? El cronómetro del servidor comenzará a correr y no se puede pausar.')) return;

    this.examenApi.iniciar(ex.id).subscribe({
      next: resp => {
        this.aplicarPreguntasYRespuestas(resp.preguntas, resp.respuestasSeleccionadas);
        this.horaFinExamen = new Date(resp.horaFin);
        this.indiceActual.set(0);
        this.fase.set('en-curso');
        this.iniciarTemporizador();
        this.intentarFullscreen();
      },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al iniciar el examen')
    });
  }

  private reanudarExamenEnCurso(ex: ExamenAplicacionResponse): void {
    this.examen.set(ex);
    this.fase.set('en-curso');
    this.examenApi.preguntasAsignadas(ex.id).subscribe({
      next: resp => {
        this.aplicarPreguntasYRespuestas(resp.preguntas, resp.respuestasSeleccionadas);
        this.horaFinExamen = new Date(resp.horaFin);
        this.iniciarTemporizador();
      },
      error: () => this.error.set('No se pudo recuperar el examen en curso')
    });
  }

  private aplicarPreguntasYRespuestas(preguntas: PreguntaExamenDto[], seleccionadas: Record<number, number>): void {
    this.preguntas.set(preguntas);
    const iniciales: Record<number, number | null> = {};
    preguntas.forEach(p => iniciales[p.idPregunta] = seleccionadas?.[p.idPregunta] ?? null);
    this.respuestas.set(iniciales);
  }

  seleccionarPregunta(i: number): void {
    this.indiceActual.set(i);
  }

  siguiente(): void {
    if (this.indiceActual() < this.preguntas().length - 1) this.indiceActual.update(i => i + 1);
  }

  anterior(): void {
    if (this.indiceActual() > 0) this.indiceActual.update(i => i - 1);
  }

  responder(idPregunta: number, idOpcion: number): void {
    const ex = this.examen();
    if (!ex) return;
    this.respuestas.update(r => ({ ...r, [idPregunta]: idOpcion }));
    this.examenApi.responder(ex.id, { preguntaId: idPregunta, opcionSeleccionadaId: idOpcion }).subscribe({
      error: (err: any) => {
        if (err?.status === 409) {
          this.error.set('El tiempo del examen venció; se finalizó automáticamente');
          this.obtenerResultado(ex.id);
        }
      }
    });
  }

  estaRespondida(idPregunta: number): boolean {
    return !!this.respuestas()[idPregunta];
  }

  confirmarFinalizar(): void {
    const sinResponder = this.totalSinResponder;
    if (!confirm(`¿Estás seguro? Esta acción no se puede deshacer. Preguntas sin responder: ${sinResponder}`)) return;
    this.finalizarExamen();
  }

  private finalizarExamen(): void {
    const ex = this.examen();
    if (!ex || this.finalizando) return;
    this.finalizando = true;
    this.examenApi.finalizar(ex.id).pipe(finalize(() => this.finalizando = false)).subscribe({
      next: resultado => {
        this.resultado.set(resultado);
        this.fase.set('resultado');
        this.detenerTemporizador();
        this.salirFullscreen();
      },
      error: (err: any) => this.error.set(err?.error?.message ?? 'Error al finalizar el examen')
    });
  }

  private obtenerResultado(idExamen: number): void {
    this.examenApi.resultado(idExamen).subscribe({
      next: r => { this.resultado.set(r); this.fase.set('resultado'); this.detenerTemporizador(); this.salirFullscreen(); },
      error: () => {}
    });
  }

  private iniciarTemporizador(): void {
    this.actualizarTiempoRestante();
    this.timerId = setInterval(() => {
      this.actualizarTiempoRestante();
      if (this.tiempoRestanteMs() <= 0) {
        this.detenerTemporizador();
        alert('El tiempo del examen ha vencido. Se enviará automáticamente.');
        this.finalizarExamen();
      }
    }, 1000);
  }

  private actualizarTiempoRestante(): void {
    if (!this.horaFinExamen) return;
    this.tiempoRestanteMs.set(Math.max(0, this.horaFinExamen.getTime() - Date.now()));
  }

  private detenerTemporizador(): void {
    if (this.timerId) { clearInterval(this.timerId); this.timerId = null; }
  }

  private intentarFullscreen(): void {
    const el = this.contenedorExamen?.nativeElement;
    if (el && el.requestFullscreen) {
      el.requestFullscreen().catch(() => {});
    }
  }

  private salirFullscreen(): void {
    if (document.fullscreenElement) {
      document.exitFullscreen().catch(() => {});
    }
  }
}
