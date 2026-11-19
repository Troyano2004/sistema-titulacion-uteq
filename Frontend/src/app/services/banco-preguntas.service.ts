import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type DificultadPregunta = 'BASICA' | 'INTERMEDIA' | 'AVANZADA';
export type EstadoPregunta = 'ACTIVA' | 'INACTIVA' | 'EN_REVISION';

export interface OpcionRequest {
  id?: number;
  texto: string;
  esCorrecta: boolean;
  orden?: number;
}

export interface OpcionResponse {
  id: number;
  texto: string;
  esCorrecta: boolean;
  orden: number;
}

export interface PreguntaComplexivoRequest {
  idEjeTematico: number;
  idCarrera: number;
  enunciado: string;
  dificultad: DificultadPregunta;
  estado?: EstadoPregunta;
  periodoAcademico: string;
  idDocenteCreador: number;
  opciones: OpcionRequest[];
}

export interface PreguntaComplexivoResponse {
  id: number;
  idEjeTematico: number;
  nombreEjeTematico?: string;
  idCarrera: number;
  enunciado: string;
  dificultad: DificultadPregunta;
  estado: EstadoPregunta;
  idDocenteCreador: number;
  nombreDocenteCreador?: string;
  fechaCreacion: string;
  periodoAcademico: string;
  opciones: OpcionResponse[];
}

export interface EstadisticasBancoDto {
  idCarrera: number;
  totalActivas: number;
  porEjeTematico: Record<string, number>;
  minimoRequerido: number;
  cumpleMinimo: boolean;
}

@Injectable({ providedIn: 'root' })
export class BancoPreguntasService {
  private readonly baseUrl = 'http://localhost:8080/api/banco-preguntas';

  constructor(private http: HttpClient) {}

  crear(req: PreguntaComplexivoRequest): Observable<PreguntaComplexivoResponse> {
    return this.http.post<PreguntaComplexivoResponse>(this.baseUrl, req);
  }

  actualizar(id: number, req: PreguntaComplexivoRequest): Observable<PreguntaComplexivoResponse> {
    return this.http.put<PreguntaComplexivoResponse>(`${this.baseUrl}/${id}`, req);
  }

  desactivar(id: number): Observable<PreguntaComplexivoResponse> {
    return this.http.delete<PreguntaComplexivoResponse>(`${this.baseUrl}/${id}`);
  }

  listar(idCarrera: number, ejeTematico?: number, estado?: string): Observable<PreguntaComplexivoResponse[]> {
    const params: string[] = [];
    if (ejeTematico) params.push(`ejeTematico=${ejeTematico}`);
    if (estado) params.push(`estado=${estado}`);
    const qs = params.length ? `?${params.join('&')}` : '';
    return this.http.get<PreguntaComplexivoResponse[]>(`${this.baseUrl}/carrera/${idCarrera}${qs}`);
  }

  estadisticas(idCarrera: number): Observable<EstadisticasBancoDto> {
    return this.http.get<EstadisticasBancoDto>(`${this.baseUrl}/carrera/${idCarrera}/estadisticas`);
  }
}
