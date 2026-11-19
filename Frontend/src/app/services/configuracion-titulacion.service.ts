import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type ModalidadEntrega = 'REDACCION_SISTEMA' | 'UPLOAD_PDF';

export interface ConfiguracionModalidadEntrega {
  idCarrera: number;
  idPeriodo: number;
  modalidadEntrega: ModalidadEntrega;
}

@Injectable({ providedIn: 'root' })
export class ConfiguracionTitulacionService {
  private readonly baseUrl = 'http://localhost:8080/api/configuracion-titulacion';

  constructor(private http: HttpClient) {}

  obtenerModalidadEntrega(idCarrera: number, idPeriodo: number): Observable<ConfiguracionModalidadEntrega> {
    return this.http.get<ConfiguracionModalidadEntrega>(
      `${this.baseUrl}/modalidad-entrega?idCarrera=${idCarrera}&idPeriodo=${idPeriodo}`
    );
  }

  configurarModalidadEntrega(idDocente: number, idCarrera: number, idPeriodo: number, modalidadEntrega: ModalidadEntrega): Observable<ConfiguracionModalidadEntrega> {
    return this.http.put<ConfiguracionModalidadEntrega>(`${this.baseUrl}/modalidad-entrega`, {
      idDocente, idCarrera, idPeriodo, modalidadEntrega
    });
  }
}
