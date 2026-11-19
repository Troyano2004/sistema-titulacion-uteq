import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DocenteSimple {
  idDocente: number;
  nombreCompleto: string;
  cedula: string | null;
}

export interface CarreraSimple {
  idCarrera: number;
  nombre: string;
}

export interface DocenteConCarreras {
  idDocente: number;
  nombreCompleto: string;
  cedula: string | null;
  carreras: CarreraSimple[];
}

@Injectable({ providedIn: 'root' })
export class DocentesService {
  private readonly base = 'http://localhost:8080/api/docentes';

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<DocenteSimple[]> {
    return this.http.get<DocenteSimple[]>(this.base, { withCredentials: true });
  }

  listarConCarreras(): Observable<DocenteConCarreras[]> {
    return this.http.get<DocenteConCarreras[]>(`${this.base}/con-carreras`, { withCredentials: true });
  }

  carrerasDe(idDocente: number): Observable<CarreraSimple[]> {
    return this.http.get<CarreraSimple[]>(`${this.base}/${idDocente}/carreras`, { withCredentials: true });
  }

  asignarCarrera(idDocente: number, idCarrera: number): Observable<CarreraSimple[]> {
    return this.http.post<CarreraSimple[]>(`${this.base}/${idDocente}/carreras`, { idCarrera }, { withCredentials: true });
  }

  quitarCarrera(idDocente: number, idCarrera: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${idDocente}/carreras/${idCarrera}`, { withCredentials: true });
  }
}
