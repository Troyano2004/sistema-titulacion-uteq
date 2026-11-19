export interface PeriodoSelect { idPeriodo: number; descripcion: string; fechaInicio: string; fechaFin: string; activo: boolean; }
export interface EstudianteSelect { idEstudiante: number; usuario: { nombres: string; apellidos: string; cedula: string; }; carrera?: { nombre: string }; }

export interface ModalidadResultado {
  modalidad: string;
  total: number;
  aprobados: number;
  reprobados: number;
  promedioCalificacion: number | null;
}
export interface CarreraResultado {
  carrera: string;
  totalEstudiantes: number;
  tasaAprobacion: number;
  porModalidad: ModalidadResultado[];
}
export interface ResultadosFacultad {
  facultad: string;
  periodo: string;
  fechaGeneracion: string;
  fechaLimitePlazoArt40: string | null;
  dentroDePlazo: boolean;
  totalEstudiantes: number;
  totalAprobados: number;
  totalReprobados: number;
  tasaAprobacionGeneral: number;
  carreras: CarreraResultado[];
}

export interface EventoTrazabilidad {
  etapa: string;
  fecha: string | null;
  actor: string;
  estadoResultante: string | null;
  detalle: string | null;
}
export interface TrazabilidadEstudiante {
  idEstudiante: number;
  estudiante: string;
  cedula: string;
  carrera: string;
  eventos: EventoTrazabilidad[];
}

export interface ConteoDto { etiqueta: string; cantidad: number; }
export interface ModalidadConteoDto { modalidad: string; total: number; aprobados: number; reprobados: number; enProceso: number; }
export interface DashboardReportes {
  periodo: string;
  totalMatriculados: number;
  matriculadosPorNivel: ConteoDto[];
  enProgreso: number;
  aprobados: number;
  reprobados: number;
  tasaAprobacion: number;
  promedioNotaGrado: number | null;
  tiempoPromedioTitulacionDias: number | null;
  porModalidad: ModalidadConteoDto[];
}

export interface DocumentoFaltanteDto { documento: string; vecesFaltante: number; }
export interface ReporteExpedientes {
  carrera: string | null;
  periodo: string | null;
  totalExpedientes: number;
  incompletos: number;
  completos: number;
  validados: number;
  legalizados: number;
  documentosMasFaltantes: DocumentoFaltanteDto[];
}
