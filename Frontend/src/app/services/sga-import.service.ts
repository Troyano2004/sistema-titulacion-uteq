import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ImportacionResultado {
  tipo: string;
  nombreArchivo: string;
  totalFilas: number;
  filasOk: number;
  filasError: number;
  errores: string[];
  fechaImportacion: string;
}

export interface ImportacionLog {
  id: number;
  tipo: string;
  nombreArchivo: string;
  totalFilas: number;
  filasOk: number;
  filasError: number;
  importadoPorNombre?: string;
  fechaImportacion: string;
}

@Injectable({ providedIn: 'root' })
export class SgaImportService {
  private readonly baseUrl = 'http://localhost:8080/api/sga';

  constructor(private http: HttpClient) {}

  private subir(endpoint: string, file: File, idUsuarioImporta?: number): Observable<ImportacionResultado> {
    const formData = new FormData();
    formData.append('file', file);
    const qs = idUsuarioImporta ? `?idUsuarioImporta=${idUsuarioImporta}` : '';
    return this.http.post<ImportacionResultado>(`${this.baseUrl}/${endpoint}${qs}`, formData);
  }

  importarPensum(file: File, idUsuarioImporta?: number): Observable<ImportacionResultado> {
    return this.subir('importar-pensum', file, idUsuarioImporta);
  }

  importarRequisitos(file: File, idUsuarioImporta?: number): Observable<ImportacionResultado> {
    return this.subir('importar-requisitos', file, idUsuarioImporta);
  }

  importarDeudas(file: File, idUsuarioImporta?: number): Observable<ImportacionResultado> {
    return this.subir('importar-deudas', file, idUsuarioImporta);
  }

  historial(): Observable<ImportacionLog[]> {
    return this.http.get<ImportacionLog[]>(`${this.baseUrl}/historial`);
  }
}
