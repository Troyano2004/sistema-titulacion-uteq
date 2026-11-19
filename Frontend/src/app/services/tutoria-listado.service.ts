import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EstudianteSeleccionDto {
  idEstudiante: number;
  nombreCompleto: string;
  cedula: string;
  opcionTitulacion: string;
}

export interface GrupoOpcionDto {
  opcionTitulacion: string;
  estudiantes: EstudianteSeleccionDto[];
}

export interface ListadoSeleccionesResponse {
  idCarrera: number;
  nombreCarrera: string;
  periodoAcademico: string;
  nivelAntepenultimo: number;
  grupos: GrupoOpcionDto[];
}

@Injectable({ providedIn: 'root' })
export class TutoriaListadoService {
  private readonly baseUrl = 'http://localhost:8080/api/tutorias';

  constructor(private http: HttpClient) {}

  listadoSelecciones(carreraId: number, periodo?: string): Observable<ListadoSeleccionesResponse> {
    const params = periodo ? `?carreraId=${carreraId}&periodo=${encodeURIComponent(periodo)}` : `?carreraId=${carreraId}`;
    return this.http.get<ListadoSeleccionesResponse>(`${this.baseUrl}/listado-selecciones${params}`);
  }
}
