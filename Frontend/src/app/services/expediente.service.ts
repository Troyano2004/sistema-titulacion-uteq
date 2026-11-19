import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type EstadoDocumentoExpediente = 'PENDIENTE' | 'CARGADO' | 'VALIDADO' | 'APROBADO' | 'RECHAZADO';
export type EstadoExpediente = 'INCOMPLETO' | 'COMPLETO' | 'VALIDADO' | 'LEGALIZADO';

export interface DocumentoExpediente {
  id: number;
  codigo: string;
  nombre: string;
  descripcion?: string;
  modalidadCatalogo: string;
  orden: number;
  obligatorio: boolean;
  responsable: string;
  estado: EstadoDocumentoExpediente;
  archivoUrl?: string;
  observaciones?: string;
  validadoPorNombre?: string;
  fechaValidacion?: string;
  motivoRechazo?: string;
}

export interface Expediente {
  id: number;
  idEstudiante: number;
  nombreEstudiante?: string;
  modalidad: string;
  periodoTitulacion?: string;
  estado: EstadoExpediente;
  fechaCreacion: string;
  documentos: DocumentoExpediente[];
}

export interface ProgresoExpediente {
  idExpediente: number;
  total: number;
  pendientes: number;
  cargados: number;
  validados: number;
  aprobados: number;
  rechazados: number;
  faltantesObligatorios: number;
}

export interface AlertaExpediente {
  idExpediente: number;
  idEstudiante: number;
  nombreEstudiante: string;
  idDocumentoExpediente: number;
  codigo: string;
  nombreDocumento: string;
  responsable: string;
  fechaEvento?: string;
  diasRestantes: number;
}

@Injectable({ providedIn: 'root' })
export class ExpedienteService {
  private readonly baseUrl = 'http://localhost:8080/api/expedientes';

  constructor(private http: HttpClient) {}

  crear(idEstudiante: number, modalidad: 'TIC' | 'COMPLEXIVO', periodoTitulacion: string): Observable<Expediente> {
    return this.http.post<Expediente>(this.baseUrl, { idEstudiante, modalidad, periodoTitulacion });
  }

  obtenerPorEstudiante(idEstudiante: number): Observable<Expediente> {
    return this.http.get<Expediente>(`${this.baseUrl}/estudiante/${idEstudiante}`);
  }

  obtenerPorId(id: number): Observable<Expediente> {
    return this.http.get<Expediente>(`${this.baseUrl}/${id}`);
  }

  listar(carrera?: number, periodo?: string, estado?: string): Observable<Expediente[]> {
    const params: string[] = [];
    if (carrera) params.push(`carrera=${carrera}`);
    if (periodo) params.push(`periodo=${encodeURIComponent(periodo)}`);
    if (estado) params.push(`estado=${estado}`);
    const qs = params.length ? `?${params.join('&')}` : '';
    return this.http.get<Expediente[]>(`${this.baseUrl}${qs}`);
  }

  cargar(idDocumento: number, archivoUrl: string, observaciones?: string): Observable<DocumentoExpediente> {
    return this.http.put<DocumentoExpediente>(`${this.baseUrl}/documentos/${idDocumento}/cargar`, { archivoUrl, observaciones });
  }

  validar(idDocumento: number, idUsuarioValida: number): Observable<DocumentoExpediente> {
    return this.http.put<DocumentoExpediente>(`${this.baseUrl}/documentos/${idDocumento}/validar`, { idUsuarioValida });
  }

  rechazar(idDocumento: number, idUsuarioValida: number, motivoRechazo: string): Observable<DocumentoExpediente> {
    return this.http.put<DocumentoExpediente>(`${this.baseUrl}/documentos/${idDocumento}/rechazar`, { idUsuarioValida, motivoRechazo });
  }

  progreso(idExpediente: number): Observable<ProgresoExpediente> {
    return this.http.get<ProgresoExpediente>(`${this.baseUrl}/${idExpediente}/progreso`);
  }

  alertas(idExpediente: number): Observable<AlertaExpediente[]> {
    return this.http.get<AlertaExpediente[]>(`${this.baseUrl}/${idExpediente}/alertas`);
  }
}
