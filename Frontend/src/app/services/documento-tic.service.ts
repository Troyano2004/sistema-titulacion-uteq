import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type ModalidadEntregaTic = 'REDACCION_SISTEMA' | 'UPLOAD_PDF';
export type EstadoDocumentoTic = 'BORRADOR' | 'EN_REVISION' | 'OBSERVADO' | 'APROBADO';
export type EstadoSeccionTic = 'VACIO' | 'EN_PROGRESO' | 'COMPLETO';

export interface CrearDocumentoTicRequest {
  idEstudiante: number;
  idTipoTrabajo: number;
  idDirector?: number | null;
  modalidadEntrega: ModalidadEntregaTic;
  titulo?: string;
}

export interface SeccionTic {
  id: number;
  codigoSeccion: string;
  titulo: string;
  contenidoHtml: string | null;
  orden: number;
  obligatoria: boolean;
  autoLlenada: boolean;
  estado: EstadoSeccionTic;
}

export interface DocumentoTic {
  id: number;
  idEstudiante: number;
  nombreEstudiante?: string;
  idTipoTrabajo: number;
  nombreTipoTrabajo?: string;
  idDirector?: number;
  nombreDirector?: string;
  modalidadEntrega: ModalidadEntregaTic;
  estado: EstadoDocumentoTic;
  titulo?: string;
  ciudad?: string;
  fechaCreacion: string;
  fechaUltimaModificacion: string;
  archivoPdfUrl?: string;
  seccionesCompletas: number;
  seccionesTotales: number;
  secciones: SeccionTic[];
}

export interface VersionTic {
  id: number;
  numeroVersion: number;
  fechaVersion: string;
  creadaPor: string;
}

export interface VersionTicDetalle extends VersionTic {
  contenidoSnapshot: string;
}

export type RolAutorComentarioTic = 'DIRECTOR' | 'PRESIDENTE_TRIBUNAL' | 'VOCAL_TRIBUNAL';

export interface ComentarioTic {
  id: number;
  idSeccion: number | null;
  codigoSeccion: string | null;
  idAutor: number;
  nombreAutor: string;
  rolAutor: RolAutorComentarioTic;
  contenido: string;
  fechaCreacion: string;
  resuelto: boolean;
}

@Injectable({ providedIn: 'root' })
export class DocumentoTicService {
  private readonly baseUrl = 'http://localhost:8080/api/documentos-tic';

  constructor(private http: HttpClient) {}

  crear(req: CrearDocumentoTicRequest): Observable<DocumentoTic> {
    return this.http.post<DocumentoTic>(this.baseUrl, req);
  }

  obtener(id: number): Observable<DocumentoTic> {
    return this.http.get<DocumentoTic>(`${this.baseUrl}/${id}`);
  }

  obtenerPorEstudiante(idEstudiante: number): Observable<DocumentoTic> {
    return this.http.get<DocumentoTic>(`${this.baseUrl}/estudiante/${idEstudiante}`);
  }

  guardarSeccion(id: number, codigoSeccion: string, contenidoHtml: string): Observable<SeccionTic> {
    return this.http.put<SeccionTic>(`${this.baseUrl}/${id}/secciones/${codigoSeccion}`, { contenidoHtml });
  }

  crearSeccionPersonalizada(id: number, codigoSeccion: string, titulo: string, obligatoria: boolean): Observable<SeccionTic> {
    return this.http.post<SeccionTic>(`${this.baseUrl}/${id}/secciones`, { codigoSeccion, titulo, obligatoria });
  }

  eliminarSeccion(id: number, codigoSeccion: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}/secciones/${codigoSeccion}`);
  }

  crearVersion(id: number, creadaPor: string): Observable<VersionTic> {
    return this.http.post<VersionTic>(`${this.baseUrl}/${id}/versiones`, { creadaPor });
  }

  listarVersiones(id: number): Observable<VersionTic[]> {
    return this.http.get<VersionTic[]>(`${this.baseUrl}/${id}/versiones`);
  }

  obtenerVersion(id: number, versionId: number): Observable<VersionTicDetalle> {
    return this.http.get<VersionTicDetalle>(`${this.baseUrl}/${id}/versiones/${versionId}`);
  }

  agregarComentario(id: number, idSeccion: number | null, idUsuarioAutor: number, rolAutor: RolAutorComentarioTic, contenido: string): Observable<ComentarioTic> {
    return this.http.post<ComentarioTic>(`${this.baseUrl}/${id}/comentarios`, { idSeccion, idUsuarioAutor, rolAutor, contenido });
  }

  listarComentarios(id: number): Observable<ComentarioTic[]> {
    return this.http.get<ComentarioTic[]>(`${this.baseUrl}/${id}/comentarios`);
  }

  resolverComentario(id: number, idComentario: number): Observable<ComentarioTic> {
    return this.http.put<ComentarioTic>(`${this.baseUrl}/${id}/comentarios/${idComentario}/resolver`, {});
  }

  registrarArchivoPdf(id: number, archivoPdfUrl: string): Observable<DocumentoTic> {
    return this.http.put<DocumentoTic>(`${this.baseUrl}/${id}/archivo-pdf`, { archivoPdfUrl });
  }

  exportarPdf(id: number): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/${id}/exportar-pdf`, {}, { responseType: 'blob' });
  }

  exportarDocx(id: number): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/${id}/exportar-docx`, {}, { responseType: 'blob' });
  }
}
