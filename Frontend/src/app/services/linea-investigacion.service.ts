import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type EstadoLineaInvestigacion = 'ACTIVA' | 'INACTIVA';

export interface LineaInvestigacion {
  id?: number;
  idCarrera: number;
  nombreCarrera?: string;
  nombre: string;
  descripcion?: string;
  estado?: EstadoLineaInvestigacion;
  fechaAprobacion?: string;
}

@Injectable({ providedIn: 'root' })
export class LineaInvestigacionService {
  private readonly baseUrl = 'http://localhost:8080/api/lineas-investigacion';

  constructor(private http: HttpClient) {}

  listarPorCarrera(idCarrera: number): Observable<LineaInvestigacion[]> {
    return this.http.get<LineaInvestigacion[]>(`${this.baseUrl}/carrera/${idCarrera}`);
  }

  crear(data: LineaInvestigacion): Observable<LineaInvestigacion> {
    return this.http.post<LineaInvestigacion>(this.baseUrl, data);
  }

  actualizar(id: number, data: LineaInvestigacion): Observable<LineaInvestigacion> {
    return this.http.put<LineaInvestigacion>(`${this.baseUrl}/${id}`, data);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
