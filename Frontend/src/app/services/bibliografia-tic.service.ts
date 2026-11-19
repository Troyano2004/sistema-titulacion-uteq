import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TipoReferencia = 'LIBRO' | 'ARTICULO' | 'TESIS' | 'WEB' | 'CONFERENCIA' | 'OTRO';

export interface ReferenciaBibliografica {
  id: number;
  autores: string;
  titulo: string;
  anioPublicacion: number;
  fuente?: string;
  tipo: TipoReferencia;
}

export interface ReferenciaBibliograficaRequest {
  autores: string;
  titulo: string;
  anioPublicacion: number;
  fuente?: string;
  tipo: TipoReferencia;
}

export interface BibliografiaFormateada {
  estandar: string;
  referenciasFormateadas: string[];
  total: number;
  minimoRequerido: number;
  cumpleMinimo: boolean;
  porcentajeRecientes: number;
  porcentajeMinimoRecientes: number;
  cumpleRecientes: boolean;
}

@Injectable({ providedIn: 'root' })
export class BibliografiaTicService {
  private readonly baseUrl = 'http://localhost:8080/api/documentos-tic';

  constructor(private http: HttpClient) {}

  listar(idDocumento: number): Observable<ReferenciaBibliografica[]> {
    return this.http.get<ReferenciaBibliografica[]>(`${this.baseUrl}/${idDocumento}/bibliografia`);
  }

  crear(idDocumento: number, req: ReferenciaBibliograficaRequest): Observable<ReferenciaBibliografica> {
    return this.http.post<ReferenciaBibliografica>(`${this.baseUrl}/${idDocumento}/bibliografia`, req);
  }

  actualizar(idDocumento: number, idReferencia: number, req: ReferenciaBibliograficaRequest): Observable<ReferenciaBibliografica> {
    return this.http.put<ReferenciaBibliografica>(`${this.baseUrl}/${idDocumento}/bibliografia/${idReferencia}`, req);
  }

  eliminar(idDocumento: number, idReferencia: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${idDocumento}/bibliografia/${idReferencia}`);
  }

  formateada(idDocumento: number, estandar?: string): Observable<BibliografiaFormateada> {
    const qs = estandar ? `?estandar=${estandar}` : '';
    return this.http.get<BibliografiaFormateada>(`${this.baseUrl}/${idDocumento}/bibliografia/formateada${qs}`);
  }
}
