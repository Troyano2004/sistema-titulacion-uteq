import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EventoCalendario {
  tipoEvento: string;
  fecha: string;
  hora?: string;
  lugar?: string;
  descripcion: string;
  idCarrera?: number;
  nombreCarrera?: string;
  estudiantes: string[];
  docentes: string[];
}

export interface PeriodoAval {
  idPeriodo: number;
  periodo: string;
  avalUnidadRegistro: boolean;
  fechaAval?: string;
  avaladoPor?: string;
}

@Injectable({ providedIn: 'root' })
export class CalendarioTitulacionService {
  private readonly baseUrl = 'http://localhost:8080/api/calendario-titulacion';

  constructor(private http: HttpClient) {}

  obtenerEventos(periodo?: string, facultad?: number): Observable<EventoCalendario[]> {
    const params: string[] = [];
    if (periodo) params.push(`periodo=${encodeURIComponent(periodo)}`);
    if (facultad) params.push(`facultad=${facultad}`);
    const qs = params.length ? `?${params.join('&')}` : '';
    return this.http.get<EventoCalendario[]>(`${this.baseUrl}${qs}`);
  }

  obtenerAval(idPeriodo: number): Observable<PeriodoAval> {
    return this.http.get<PeriodoAval>(`${this.baseUrl}/periodo/${idPeriodo}/aval`);
  }

  darAval(idPeriodo: number, avaladoPor: string): Observable<PeriodoAval> {
    return this.http.put<PeriodoAval>(`${this.baseUrl}/periodo/${idPeriodo}/aval`, { avaladoPor });
  }
}
