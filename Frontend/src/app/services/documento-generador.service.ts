import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type PlantillaKey =
  | 'portada' | 'autoria' | 'certificacion-director' | 'certificado-antiplagio'
  | 'pagina-tribunal' | 'codigo-dublin' | 'informe-favorable' | 'certificacion-tribunal'
  | 'certificado-redaccion' | 'acta-sustentacion' | 'acta-grado';

export const PLANTILLAS: { key: PlantillaKey; nombre: string }[] = [
  { key: 'portada', nombre: 'Portada' },
  { key: 'autoria', nombre: 'Declaración de Autoría' },
  { key: 'certificacion-director', nombre: 'Certificación de Culminación (Director)' },
  { key: 'certificado-antiplagio', nombre: 'Certificado Antiplagio' },
  { key: 'pagina-tribunal', nombre: 'Página del Tribunal' },
  { key: 'codigo-dublin', nombre: 'Código Dublín' },
  { key: 'informe-favorable', nombre: 'Informe Favorable del Director' },
  { key: 'certificacion-tribunal', nombre: 'Certificación del Tribunal' },
  { key: 'certificado-redaccion', nombre: 'Certificado de Redacción Técnica' },
  { key: 'acta-sustentacion', nombre: 'Acta de Sustentación' },
  { key: 'acta-grado', nombre: 'Acta de Grado' },
];

@Injectable({ providedIn: 'root' })
export class DocumentoGeneradorService {
  private readonly baseUrl = 'http://localhost:8080/api/documentos/generar';

  constructor(private http: HttpClient) {}

  generar(plantilla: PlantillaKey, idEstudiante: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${plantilla}/${idEstudiante}`, { responseType: 'blob' });
  }
}
