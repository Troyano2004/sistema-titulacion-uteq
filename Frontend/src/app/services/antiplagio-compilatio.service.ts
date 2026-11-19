import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RegistrarResultadoAntiplagioRequest {
  estudianteId: number;
  porcentaje: number;
  observaciones?: string;
  archivoReporteId?: number;
}

export interface AntiplagioResultadoResponse {
  idIntento: number;
  idProyecto: number;
  porcentaje: number;
  favorable: boolean;
  mensaje: string;
  fecha: string;
}

export interface CertificadoCompilatioResponse {
  idIntento: number;
  idProyecto: number;
  porcentaje: number;
  certificadoEmitido: boolean;
  fechaEmision: string;
  firmadoPor: string | null;
}

@Injectable({ providedIn: 'root' })
export class AntiplagioCompilatioService {
  private readonly baseUrl = 'http://localhost:8080/api/antiplagio';

  constructor(private http: HttpClient) {}

  registrarResultado(req: RegistrarResultadoAntiplagioRequest): Observable<AntiplagioResultadoResponse> {
    return this.http.post<AntiplagioResultadoResponse>(`${this.baseUrl}/registrar-resultado`, req);
  }

  emitirCertificado(estudianteId: number): Observable<CertificadoCompilatioResponse> {
    return this.http.post<CertificadoCompilatioResponse>(`${this.baseUrl}/emitir-certificado/${estudianteId}`, {});
  }
}
