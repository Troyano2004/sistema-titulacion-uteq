import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type EstadoEjeTematico = 'ACTIVO' | 'INACTIVO';

export interface EjeTematico {
  id?: number;
  idCarrera: number;
  nombreCarrera?: string;
  nombre: string;
  descripcion?: string;
  idDocenteResponsable?: number;
  nombreDocenteResponsable?: string;
  periodoAcademico: string;
  estado?: EstadoEjeTematico;
}

@Injectable({ providedIn: 'root' })
export class EjeTematicoService {
  private readonly baseUrl = 'http://localhost:8080/api/ejes-tematicos';

  constructor(private http: HttpClient) {}

  listarPorCarrera(idCarrera: number, periodo?: string): Observable<EjeTematico[]> {
    const url = periodo
      ? `${this.baseUrl}/carrera/${idCarrera}?periodo=${encodeURIComponent(periodo)}`
      : `${this.baseUrl}/carrera/${idCarrera}`;
    return this.http.get<EjeTematico[]>(url);
  }

  crear(data: EjeTematico): Observable<EjeTematico> {
    return this.http.post<EjeTematico>(this.baseUrl, data);
  }

  actualizar(id: number, data: EjeTematico): Observable<EjeTematico> {
    return this.http.put<EjeTematico>(`${this.baseUrl}/${id}`, data);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
