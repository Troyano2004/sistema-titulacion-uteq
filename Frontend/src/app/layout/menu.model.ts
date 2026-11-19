export type AppRole =
  | 'ADMIN'
  | 'DOCENTE'
  | 'DOCENTE_TITULADO'
  | 'ESTUDIANTE'
  | 'COORDINADOR'
  | 'DECANO'
  | 'SECRETARIO_ABOGADO'
  | 'DIRECCION_ACADEMICA'
  | 'GESTOR_USUARIOS'
  | 'ROL_REPORT';

export interface MenuItem {
  label: string;
  path: string;
  roles?: AppRole[];
  soloComplexivo?: boolean;
  soloNoComplexivo?: boolean;
}

export interface MenuSection {
  title: string;
  icon: string;
  description?: string;
  roles?: AppRole[];
  soloComplexivo?: boolean;
  soloNoComplexivo?: boolean;
  items: MenuItem[];
}

export interface SearchResult {
  label: string;
  section: string;
  icon: string;
  path: string;
}
