import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, shareReplay } from 'rxjs';

export interface EstudiantePickerItem {
  idEstudiante: number;
  nombreCompleto: string;
  cedula: string;
  carrera: string | null;
  nivelSemestre: number | null;
}

interface EstudianteRaw {
  idEstudiante: number;
  usuario?: { nombres?: string | null; apellidos?: string | null; cedula?: string | null } | null;
  carrera?: { nombre?: string | null } | null;
  nivelSemestre?: number | null;
}

/**
 * Fuente de datos del selector de estudiante reutilizable (ver
 * SelectorEstudianteComponent). Usa el mismo endpoint que ya consume el
 * módulo de Reportes (GET /api/reportes/estudiantes) — no se duplica la
 * consulta en el backend, solo se mapea a una forma más chica y estable
 * para no acoplar el componente reutilizable a la entidad completa.
 */
@Injectable({ providedIn: 'root' })
export class EstudianteSelectorService {
  private readonly url = 'http://localhost:8080/api/reportes/estudiantes';
  private cache$?: Observable<EstudiantePickerItem[]>;

  constructor(private readonly http: HttpClient) {}

  listar(): Observable<EstudiantePickerItem[]> {
    if (!this.cache$) {
      this.cache$ = this.http.get<EstudianteRaw[]>(this.url, { withCredentials: true }).pipe(
        map(rows => (rows || []).map(this.aPickerItem)),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }
    return this.cache$;
  }

  private aPickerItem(r: EstudianteRaw): EstudiantePickerItem {
    const nombreCompleto = [r.usuario?.nombres, r.usuario?.apellidos]
      .map(v => (v ?? '').trim())
      .filter(v => v.length > 0)
      .join(' ') || 'Sin nombre';
    return {
      idEstudiante: r.idEstudiante,
      nombreCompleto,
      cedula: r.usuario?.cedula ?? '',
      carrera: r.carrera?.nombre ?? null,
      nivelSemestre: r.nivelSemestre ?? null,
    };
  }
}
