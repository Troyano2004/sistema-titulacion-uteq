import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TipoExamenComplexivo = 'FINAL' | 'SUPLETORIO';
export type EstadoExamenComplexivo = 'PROGRAMADO' | 'EN_CURSO' | 'FINALIZADO' | 'ANULADO' | 'NO_PRESENTADO';

export interface ProgramarExamenRequest {
  idEstudiante: number;
  tipo: TipoExamenComplexivo;
  fechaProgramada: string;
}

export interface ExamenAplicacionResponse {
  id: number;
  idEstudiante: number;
  nombreEstudiante?: string;
  idCarrera?: number;
  nombreCarrera?: string;
  idPeriodo?: number;
  tipo: TipoExamenComplexivo;
  fechaProgramada: string;
  horaInicio?: string;
  horaFin?: string;
  duracionMinutos: number;
  estado: EstadoExamenComplexivo;
  calificacion?: number;
  totalPreguntasCorrectas?: number;
  idDelegado?: number;
  nombreDelegado?: string;
  observaciones?: string;
}

export interface RegistrarIngresoRequest {
  documentoIdentidad: string;
  firmadoCompromisosEticos: boolean;
}

export interface OpcionExamenDto {
  idOpcion: number;
  texto: string;
  orden: number;
}

export interface PreguntaExamenDto {
  idPregunta: number;
  enunciado: string;
  opciones: OpcionExamenDto[];
}

export interface IniciarExamenResponse {
  idExamen: number;
  horaInicio: string;
  horaFin: string;
  duracionMinutos: number;
  preguntas: PreguntaExamenDto[];
  respuestasSeleccionadas: Record<number, number>;
}

export interface ResponderPreguntaRequest {
  preguntaId: number;
  opcionSeleccionadaId: number | null;
}

export interface ResultadoExamenDto {
  idExamen: number;
  calificacion: number;
  totalPreguntasCorrectas: number;
  totalPreguntas: number;
  aprobado: boolean;
}

export interface DelegadoDisponibleDto {
  idDocente: number;
  nombreDocente?: string;
  idCarrera: number;
  nombreCarrera?: string;
}

export interface RegistrarFraudeRequest {
  descripcionIncidente: string;
  evidencia?: string;
}

@Injectable({ providedIn: 'root' })
export class ExamenComplexivoService {
  private readonly baseUrl = 'http://localhost:8080/api/examen-complexivo';

  constructor(private http: HttpClient) {}

  programar(req: ProgramarExamenRequest): Observable<ExamenAplicacionResponse> {
    return this.http.post<ExamenAplicacionResponse>(`${this.baseUrl}/programar`, req);
  }

  delegadosDisponibles(carreraEstudiante: number): Observable<DelegadoDisponibleDto[]> {
    return this.http.get<DelegadoDisponibleDto[]>(`${this.baseUrl}/delegados-disponibles?carreraEstudiante=${carreraEstudiante}`);
  }

  asignarDelegado(idExamen: number, idDocente: number): Observable<ExamenAplicacionResponse> {
    return this.http.post<ExamenAplicacionResponse>(`${this.baseUrl}/${idExamen}/asignar-delegado`, { idDocente });
  }

  obtener(idExamen: number): Observable<ExamenAplicacionResponse> {
    return this.http.get<ExamenAplicacionResponse>(`${this.baseUrl}/${idExamen}`);
  }

  listarPorEstudiante(idEstudiante: number): Observable<ExamenAplicacionResponse[]> {
    return this.http.get<ExamenAplicacionResponse[]>(`${this.baseUrl}/estudiante/${idEstudiante}`);
  }

  registrarIngreso(idExamen: number, req: RegistrarIngresoRequest): Observable<ExamenAplicacionResponse> {
    return this.http.post<ExamenAplicacionResponse>(`${this.baseUrl}/${idExamen}/registrar-ingreso`, req);
  }

  registrarFraude(idExamen: number, req: RegistrarFraudeRequest): Observable<ExamenAplicacionResponse> {
    return this.http.post<ExamenAplicacionResponse>(`${this.baseUrl}/${idExamen}/registrar-fraude`, req);
  }

  iniciar(idExamen: number): Observable<IniciarExamenResponse> {
    return this.http.post<IniciarExamenResponse>(`${this.baseUrl}/${idExamen}/iniciar`, {});
  }

  preguntasAsignadas(idExamen: number): Observable<IniciarExamenResponse> {
    return this.http.get<IniciarExamenResponse>(`${this.baseUrl}/${idExamen}/preguntas-asignadas`);
  }

  responder(idExamen: number, req: ResponderPreguntaRequest): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${idExamen}/responder`, req);
  }

  finalizar(idExamen: number): Observable<ResultadoExamenDto> {
    return this.http.post<ResultadoExamenDto>(`${this.baseUrl}/${idExamen}/finalizar`, {});
  }

  resultado(idExamen: number): Observable<ResultadoExamenDto> {
    return this.http.get<ResultadoExamenDto>(`${this.baseUrl}/${idExamen}/resultado`);
  }
}
