import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface VerificacionMensual {
  estudianteId: number;
  mes: number;
  anio: number;
  totalReuniones: number;
  cumplioMinimo: boolean;
}

@Injectable({ providedIn: 'root' })
export class AsesoriaVerificacionService {
  private readonly baseUrl = 'http://localhost:8080/api/asesorias';

  constructor(private http: HttpClient) {}

  verificarCumplimiento(estudianteId: number, mes: number, anio: number): Observable<VerificacionMensual> {
    return this.http.get<VerificacionMensual>(
      `${this.baseUrl}/verificar-cumplimiento?estudianteId=${estudianteId}&mes=${mes}&anio=${anio}`
    );
  }
}
