import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type NivelMatricula = 'TITULACION_I' | 'TITULACION_II';
export type EstadoMatricula = 'ACTIVA' | 'COMPLETADA' | 'ANULADA';

export interface MatriculaTitulacion {
  id?: number;
  idEstudiante: number;
  nombreEstudiante?: string;
  periodoAcademico: string;
  nivel: NivelMatricula;
  idOpcionTitulacion: number;
  nombreOpcionTitulacion?: string;
  idSubtipoTrabajo?: number;
  nombreSubtipoTrabajo?: string;
  fechaMatricula?: string;
  estado?: EstadoMatricula;
  numeroCambiosOpcion?: number;
  motivoUltimoCambio?: string;
  fechaUltimoCambio?: string;
  documentoAdjuntoIdCasoMedico?: number;
}

export interface MatriculaTitulacionRequest {
  idEstudiante: number;
  periodoAcademico: string;
  nivel: NivelMatricula;
  idOpcionTitulacion: number;
  idSubtipoTrabajo?: number;
  // Art. 18 reglamento UIC-UTEQ — solo se validan para nivel TITULACION_II
  idiomaExtranjeroAprobado?: boolean;
  practicasLaboralesAprobadas?: boolean;
  servicioComunitarioAprobado?: boolean;
}

export interface CambiarOpcionMatriculaRequest {
  idOpcionTitulacion: number;
  idSubtipoTrabajo?: number;
  motivo?: string;
  semanaActual: number;
  motivoMedico?: string;
  documentoAdjuntoId?: number;
}

@Injectable({ providedIn: 'root' })
export class MatriculaTitulacionService {
  private readonly baseUrl = 'http://localhost:8080/api/matriculas-titulacion';

  constructor(private http: HttpClient) {}

  listarPorEstudiante(idEstudiante: number): Observable<MatriculaTitulacion[]> {
    return this.http.get<MatriculaTitulacion[]>(`${this.baseUrl}/estudiante/${idEstudiante}`);
  }

  crear(data: MatriculaTitulacionRequest): Observable<MatriculaTitulacion> {
    return this.http.post<MatriculaTitulacion>(this.baseUrl, data);
  }

  cambiarOpcion(id: number, data: CambiarOpcionMatriculaRequest): Observable<MatriculaTitulacion> {
    return this.http.put<MatriculaTitulacion>(`${this.baseUrl}/${id}/cambiar-opcion`, data);
  }
}
