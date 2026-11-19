import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ActaGradoIncorporacion {
  idActa: number;
  idEstudiante: number;
  nombreEstudiante?: string;
  tipoTitulacion?: string;
  modalidad?: string;
  promedioRecordAcademico?: number;
  notaTitulacion?: number;
  notaFinalGrado?: number;
  estado: string;
  firmaRector: boolean;
  firmaDecano: boolean;
  firmaSecretarioGeneral: boolean;
  fechaCeremonia?: string;
  fechaEmision?: string;
}

export interface PendienteIncorporacion {
  idActa: number;
  idEstudiante: number;
  nombreEstudiante: string;
  idCarrera?: number;
  nombreCarrera?: string;
  idFacultad?: number;
  estado: string;
  firmaRector: boolean;
  firmaDecano: boolean;
  firmaSecretarioGeneral: boolean;
}

export interface TituloProfesional {
  id: number;
  idEstudiante: number;
  nombreEstudiante?: string;
  idActa: number;
  numeroTitulo?: string;
  fechaEmision?: string;
  registroSenescyt?: string;
  fechaRegistroSenescyt?: string;
  estado: string;
}

export type TipoFirma = 'RECTOR' | 'DECANO' | 'SECRETARIO_GENERAL';

@Injectable({ providedIn: 'root' })
export class IncorporacionService {
  private readonly baseActas = 'http://localhost:8080/api/actas-grado';
  private readonly baseTitulos = 'http://localhost:8080/api/titulos';
  private readonly baseIncorporacion = 'http://localhost:8080/api/incorporacion';

  constructor(private http: HttpClient) {}

  pendientes(idFacultad?: number): Observable<PendienteIncorporacion[]> {
    const qs = idFacultad ? `?facultad=${idFacultad}` : '';
    return this.http.get<PendienteIncorporacion[]>(`${this.baseIncorporacion}/pendientes${qs}`);
  }

  generar(idEstudiante: number): Observable<ActaGradoIncorporacion> {
    return this.http.post<ActaGradoIncorporacion>(`${this.baseActas}/generar/${idEstudiante}`, {});
  }

  obtenerPorEstudiante(idEstudiante: number): Observable<ActaGradoIncorporacion> {
    return this.http.get<ActaGradoIncorporacion>(`${this.baseActas}/estudiante/${idEstudiante}`);
  }

  firmar(idActa: number, tipoFirma: TipoFirma): Observable<ActaGradoIncorporacion> {
    return this.http.put<ActaGradoIncorporacion>(`${this.baseActas}/${idActa}/firmar`, { tipoFirma });
  }

  programarCeremonia(idActa: number, fechaCeremonia: string): Observable<ActaGradoIncorporacion> {
    return this.http.put<ActaGradoIncorporacion>(`${this.baseActas}/${idActa}/ceremonia`, { fechaCeremonia });
  }

  emitirTitulo(idActa: number, numeroTitulo: string): Observable<TituloProfesional> {
    return this.http.post<TituloProfesional>(`${this.baseTitulos}/emitir/${idActa}`, { numeroTitulo });
  }

  registrarSenescyt(idTitulo: number, registroSenescyt: string): Observable<TituloProfesional> {
    return this.http.put<TituloProfesional>(`${this.baseTitulos}/${idTitulo}/registrar-senescyt`, { registroSenescyt });
  }

  obtenerTituloPorEstudiante(idEstudiante: number): Observable<TituloProfesional> {
    return this.http.get<TituloProfesional>(`${this.baseTitulos}/estudiante/${idEstudiante}`);
  }
}
