import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PropuestaPendiente {
  idPropuesta: number;
  titulo: string;
  estudiante: string;
  idCarrera: number;
  carrera: string;
  fechaRecepcionComision: string | null;
  diasHabilesTranscurridos: number;
}

export interface DocenteElegible {
  idDocente: number;
  nombre: string;
  tipoVinculacion: string;
  nivelPosgrado: string;
}

export interface AprobarYDesignarRequest {
  idUsuario: number;
  directorId: number;
  presidenteTribunalId: number;
  vocal1Id: number;
  vocal2Id: number;
  observaciones?: string;
}

export interface AprobarYDesignarResponse {
  idPropuesta: number;
  idProyecto: number;
  director: string;
  presidenteTribunal: string;
  vocal1: string;
  vocal2: string;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class PropuestaResolucionService {
  private readonly baseUrl = 'http://localhost:8080/api/propuestas';

  constructor(private http: HttpClient) {}

  listarPendientes(idCarrera?: number): Observable<PropuestaPendiente[]> {
    const url = idCarrera ? `${this.baseUrl}/pendientes-consejo-directivo?idCarrera=${idCarrera}` : `${this.baseUrl}/pendientes-consejo-directivo`;
    return this.http.get<PropuestaPendiente[]>(url);
  }

  listarDocentesElegibles(idCarrera: number): Observable<DocenteElegible[]> {
    return this.http.get<DocenteElegible[]>(`${this.baseUrl}/docentes-elegibles?idCarrera=${idCarrera}`);
  }

  aprobarYDesignar(idPropuesta: number, req: AprobarYDesignarRequest): Observable<AprobarYDesignarResponse> {
    return this.http.post<AprobarYDesignarResponse>(`${this.baseUrl}/${idPropuesta}/aprobar-y-designar`, req);
  }
}
